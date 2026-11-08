import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import org.xmlpull.v1.XmlSerializer;
import java.io.FileOutputStream;

/** Read-only shell helper for the disposable rooted AOSP acceptance device.
 * A live workout updates every second: sample the current native accessibility
 * tree instead of waiting for a one-second global idle interval. No app hooks,
 * replacement UI, APK installation, dependency or signing identity is needed.
 */
public final class NativeWindowDump {
    private static String value(CharSequence text) { return text == null ? "" : text.toString(); }
    private static void node(XmlSerializer xml, AccessibilityNodeInfo item, int index, int depth) throws Exception {
        if (depth > 80) throw new IllegalStateException("Accessibility hierarchy too deep");
        Rect bounds = new Rect(); item.getBoundsInScreen(bounds);
        xml.startTag(null,"node");
        xml.attribute(null,"index",Integer.toString(index));
        xml.attribute(null,"text",value(item.getText()));
        xml.attribute(null,"resource-id",value(item.getViewIdResourceName()));
        xml.attribute(null,"class",value(item.getClassName()));
        xml.attribute(null,"package",value(item.getPackageName()));
        xml.attribute(null,"content-desc",value(item.getContentDescription()));
        xml.attribute(null,"checkable",Boolean.toString(item.isCheckable()));
        xml.attribute(null,"checked",Boolean.toString(item.isChecked()));
        xml.attribute(null,"clickable",Boolean.toString(item.isClickable()));
        xml.attribute(null,"enabled",Boolean.toString(item.isEnabled()));
        xml.attribute(null,"focusable",Boolean.toString(item.isFocusable()));
        xml.attribute(null,"focused",Boolean.toString(item.isFocused()));
        xml.attribute(null,"scrollable",Boolean.toString(item.isScrollable()));
        xml.attribute(null,"long-clickable",Boolean.toString(item.isLongClickable()));
        xml.attribute(null,"password",Boolean.toString(item.isPassword()));
        xml.attribute(null,"selected",Boolean.toString(item.isSelected()));
        xml.attribute(null,"bounds",bounds.toShortString());
        for(int i=0;i<item.getChildCount();i++) {
            AccessibilityNodeInfo child=item.getChild(i);
            if(child!=null) { try { if(child.isVisibleToUser())node(xml,child,i,depth+1); } finally { child.recycle(); } }
        }
        xml.endTag(null,"node");
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Expected output XML path");
        // Android 15 AccessibilityInteractionClient also creates a main-looper Handler.
        Looper.prepareMainLooper();
        HandlerThread callbacks=new HandlerThread("gardenia-native-window");callbacks.start();
        UiAutomation automation=null;
        try {
            // Same shell-only framework connection used by Android's uiautomator command.
            Class<?> binder=Class.forName("android.app.IUiAutomationConnection");
            Object connection=Class.forName("android.app.UiAutomationConnection").getConstructor().newInstance();
            automation=(UiAutomation)UiAutomation.class.getConstructor(Looper.class,binder).newInstance(callbacks.getLooper(),connection);
            UiAutomation.class.getMethod("connect").invoke(automation);
            AccessibilityServiceInfo info=automation.getServiceInfo();
            info.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            automation.setServiceInfo(info);
            AccessibilityNodeInfo root=null;
            for(int i=0;i<100&&root==null;i++) { root=automation.getRootInActiveWindow();if(root==null)Thread.sleep(50); }
            if(root==null)throw new IllegalStateException("No current native accessibility window");
            try(FileOutputStream output=new FileOutputStream(args[0])) {
                XmlSerializer xml=Xml.newSerializer();xml.setOutput(output,"UTF-8");xml.startDocument("UTF-8",true);
                xml.startTag(null,"hierarchy");xml.attribute(null,"rotation","0");node(xml,root,0,0);xml.endTag(null,"hierarchy");xml.endDocument();
            } finally { root.recycle(); }
            System.out.println("FRESH_NATIVE_WINDOW "+args[0]);
        } finally {
            if(automation!=null)try { UiAutomation.class.getMethod("disconnect").invoke(automation); } catch(Exception ignored) { }
            callbacks.quitSafely();
        }
    }
}
