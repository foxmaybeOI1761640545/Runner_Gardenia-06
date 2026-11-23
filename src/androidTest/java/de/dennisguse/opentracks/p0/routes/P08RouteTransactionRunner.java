package de.dennisguse.opentracks.p0.routes;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.test.platform.app.InstrumentationRegistry;
import de.dennisguse.opentracks.TestRunner;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Dedicated test-APK runner: producer must be killed externally, verifier alone may report success.
 * Neither producer nor verifier is included in JUnit pass totals. Uses real private persistent files. */
public class P08RouteTransactionRunner extends TestRunner {
    private static final Set<String> CASES = Set.of("addPrepared", "addPromoted", "addCommitted", "addGarbage", "deleteRetired", "deleteCommitted", "renamePrepared", "renameCommitted");
    private File root;
    private File privateFile(String name) { return new File(getTargetContext().getFilesDir(), "p08-proof-" + name + ".json"); }
    private File routeFile(int n) { return new File(root, "routes/" + P08RouteRepositoryPlatformTest.fixture(n).id() + ".route"); }
    private void json(File target, JSONObject value) throws Exception {
        File temporary = new File(target.getPath() + ".tmp");
        try (var output = new FileOutputStream(temporary)) { output.write(value.toString(2).getBytes(StandardCharsets.UTF_8)); output.getFD().sync(); }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @Override public void onStart() {
        try {
            getUiAutomation().adoptShellPermissionIdentity(Manifest.permission.SET_TIME_ZONE, Manifest.permission.WRITE_SECURE_SETTINGS);
            try { Settings.Global.putInt(getTargetContext().getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0); getTargetContext().getSystemService(AlarmManager.class).setTimeZone("Asia/Singapore"); }
            finally { getUiAutomation().dropShellPermissionIdentity(); }
            var args = InstrumentationRegistry.getArguments(); String mode = args.getString("mode"), name = args.getString("case"), source = args.getString("buildCommit");
            require(CASES.contains(name) && source != null && source.matches("[0-9a-f]{40}"), "Exact case/source required");
            root = new File(getTargetContext().getFilesDir(), "route-library");
            if ("producer".equals(mode)) produce(name, source, args.getString("checkpoint"));
            else if ("verify".equals(mode)) verify(name, source);
            else throw new AssertionError("Unknown transaction probe mode");
        } catch (Throwable error) {
            Bundle result = new Bundle(); result.putString("p08Verified", "false"); result.putString("error", android.util.Log.getStackTraceString(error)); finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void produce(String name, String source, String checkpoint) throws Exception {
        require(checkpoint != null, "Missing checkpoint"); Files.deleteIfExists(privateFile("ready").toPath()); Files.deleteIfExists(privateFile("result").toPath());
        var repo = new RouteRepository(root);
        Set<UUID> known = new HashSet<>(); for (int n = 1; n <= 3; n++) known.add(P08RouteRepositoryPlatformTest.fixture(n).id());
        for (var entry : repo.list().entries()) { require(known.contains(entry.id()), "Refuse to delete nonfixture route"); repo.delete(entry.id()); }
        boolean deletion = name.startsWith("delete"); repo.add(deletion ? List.of(P08RouteRepositoryPlatformTest.fixture(1), P08RouteRepositoryPlatformTest.fixture(2)) : List.of(P08RouteRepositoryPlatformTest.fixture(1)), false);
        repo.acknowledgeRepairNotice(); String oldHash = RouteCodec.sha256(Files.readAllBytes(routeFile(1).toPath()));
        RouteRepository interrupted = new RouteRepository(root, at -> {
            if (checkpoint.equals(at)) {
                try {
                    json(privateFile("ready"), new JSONObject().put("source", source).put("case", name).put("checkpoint", at).put("producerPid", Process.myPid()).put("oldRouteSha256", oldHash).put("api", android.os.Build.VERSION.SDK_INT));
                } catch (Exception e) { throw new java.io.IOException("Cannot persist kill rendezvous", e); }
                // The host kills this real process while its transaction call is on the stack.
                long deadline = SystemClock.elapsedRealtime() + 45_000;
                while (SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100);
                throw new java.io.IOException("Host did not terminate producer at checkpoint");
            }
        });
        if (name.startsWith("add")) interrupted.add(List.of(P08RouteRepositoryPlatformTest.fixture(2), P08RouteRepositoryPlatformTest.fixture(3)), false);
        else if (deletion) interrupted.delete(P08RouteRepositoryPlatformTest.fixture(2).id());
        else interrupted.rename(P08RouteRepositoryPlatformTest.fixture(1).id(), "Renamed locally");
        throw new AssertionError("Producer returned instead of being killed externally");
    }
    private void verify(String name, String source) throws Exception {
        JSONObject ready = new JSONObject(new String(Files.readAllBytes(privateFile("ready").toPath()), StandardCharsets.UTF_8));
        require(source.equals(ready.getString("source")) && name.equals(ready.getString("case")), "Stale producer proof"); require(ready.getInt("producerPid") != Process.myPid(), "Verifier reused producer process");
        boolean committed = name.endsWith("Committed") || name.equals("addGarbage");
        List<Integer> numbers = name.startsWith("add") && committed ? List.of(1, 2, 3) : name.startsWith("delete") && !committed ? List.of(1, 2) : List.of(1);
        var repo = new RouteRepository(root); var snapshot = repo.verifyAll(); require(!snapshot.cleanupPending(), "Recovery still pending");
        require(snapshot.entries().size() == numbers.size(), "Partial batch, wrong route count"); JSONArray ids = new JSONArray();
        for (int n : numbers) {
            Route expected = P08RouteRepositoryPlatformTest.fixture(n); if (name.equals("renameCommitted")) expected = expected.renamed("Renamed locally");
            require(expected.equals(repo.get(expected.id())), "Recovered route contents differ"); ids.put(expected.id().toString());
        }
        require(ready.getString("oldRouteSha256").equals(RouteCodec.sha256(Files.readAllBytes(routeFile(1).toPath()))), "Existing geometry changed");
        for (int n = 1; n <= 3; n++) require(routeFile(n).exists() == numbers.contains(n), "Staged/deleted route file leaked");
        for (String folder : List.of("pending", "garbage")) { File[] contents = new File(root, folder).listFiles(); require(contents != null && contents.length == 0, "Unclean transaction directory"); }
        require(Arrays.equals(Files.readAllBytes(new File(root, "index.bin").toPath()), Files.readAllBytes(new File(root, "index.mirror").toPath())), "Index and mirror disagree");
        require(repo.repair().entries().size() == numbers.size(), "Repair resurrected removed routes");
        JSONObject result = new JSONObject().put("source", source).put("case", name).put("checkpoint", ready.getString("checkpoint")).put("producerPid", ready.getInt("producerPid")).put("recoveryPid", Process.myPid())
                .put("ids", ids).put("committed", committed).put("oldRouteSha256", ready.getString("oldRouteSha256")).put("api", android.os.Build.VERSION.SDK_INT).put("passed", true).put("persistentDirectory", "files/route-library");
        json(privateFile("result"), result); Bundle output = new Bundle(); output.putString("p08Verified", "true"); output.putString("source", source); output.putString("case", name); finish(Activity.RESULT_OK, output);
    }
}
