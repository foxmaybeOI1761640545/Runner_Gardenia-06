package de.dennisguse.opentracks.p0.map;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.Arrays;
import org.junit.*;

public class P06MapRepositoryTest {
    private final Context context=ApplicationProvider.getApplicationContext();
    private OfflineMapRepository repository;
    private InputStream fixture()throws IOException{return InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("p06/gardenia-synthetic.map");}
    @Before public void before()throws Exception{repository=new OfflineMapRepository(context);clean();}
    @After public void after()throws Exception{clean();}
    private void clean()throws Exception{for(var e:repository.read().entries())repository.delete(e.id());}
    private OfflineMapRepository.State importValid()throws Exception{try(var input=fixture()){return repository.importMap(input,"Synthetic park.map");}}
    @Test public void privateCopySurvivesClosingOriginalStreamAndNewRepository()throws Exception{
        var state=importValid();assertEquals(1,state.entries().size());assertEquals(751,state.current().bytes());
        var second=new OfflineMapRepository(context);assertEquals(state,second.read());var map=second.open(second.read().current());try{assertEquals(103.8,map.startPosition().longitude,0);}finally{map.close();}
    }
    @Test public void corruptImportRetainsCurrentMap()throws Exception{var old=importValid();try{repository.importMap(new ByteArrayInputStream(new byte[80]),"broken.map");fail();}catch(IOException expected){}assertEquals(old,repository.read());}
    @Test public void unsupportedVersionRetainsCurrentMap()throws Exception{
        var old=importValid();byte[] bytes;try(var input=fixture();var output=new ByteArrayOutputStream()){input.transferTo(output);bytes=output.toByteArray();}
        // Magic20 + header length4, then four-byte map format version.
        Arrays.fill(bytes,24,28,(byte)0x7f);
        try{repository.importMap(new ByteArrayInputStream(bytes),"future.map");fail();}catch(IOException expected){}assertEquals(old,repository.read());
    }
    @Test public void truncatedBodyIsRejected()throws Exception{byte[] bytes;try(var input=fixture()){bytes=new byte[100];assertEquals(100,input.read(bytes));}try{repository.importMap(new ByteArrayInputStream(bytes),"truncated.map");fail();}catch(IOException expected){}assertTrue(repository.read().entries().isEmpty());}
    @Test public void interruptedCopyLeavesPreviousSelectionAndNoPendingFile()throws Exception{
        var old=importValid();InputStream failing=new InputStream(){int n;public int read()throws IOException{if(n++>40)throw new IOException("Synthetic interruption");return 1;}};
        try{repository.importMap(failing,"interrupted.map");fail();}catch(IOException expected){}assertEquals(old,repository.read());assertEquals(0,new File(context.getFilesDir(),"offline-maps").listFiles((d,n)->n.endsWith(".pending")).length);
    }
    @Test public void selectAndDeleteRetainOtherMapAndOriginalWorkoutStore()throws Exception{
        var one=importValid();var two=importValid();assertEquals(2,two.entries().size());assertEquals(one.selected(),repository.select(one.selected()).selected());
        var selected=repository.delete(one.selected());assertEquals(1,selected.entries().size());assertEquals(two.selected(),selected.selected());repository.delete(two.selected());assertNull(repository.read().current());
    }
    @Test public void pathTraversalCannotSelectOrDeleteFiles()throws Exception{
        var old=importValid();try{repository.select("../../other");fail();}catch(IOException expected){}
        try{repository.delete("../../other");fail();}catch(IllegalArgumentException expected){}assertEquals(old,repository.read());
    }
}
