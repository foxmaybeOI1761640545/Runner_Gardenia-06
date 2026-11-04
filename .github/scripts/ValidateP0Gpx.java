import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

/** Host-only JDK GPX validation; never downloads remote schemas or DTDs. */
class ValidateP0Gpx {
    public static void main(String[] args) throws Exception {
        if(args.length<2)throw new IllegalArgumentException("Schema and actual Android GPX files required");
        var factory=SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        var schema=factory.newSchema(new File(args[0]));
        for(int i=1;i<args.length;i++) {
            var validator=schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            validator.validate(new StreamSource(new File(args[i])));
            System.out.println("Validated GPX 1.1: "+new File(args[i]).getName());
        }
    }
}
