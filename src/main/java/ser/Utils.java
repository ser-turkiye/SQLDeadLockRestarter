package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IBpmService;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.IProcessType;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.foldermanager.IFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Utils {
    static Logger log = LogManager.getLogger();
    static ISession session = null;
    static IDocumentServer server = null;
    static IBpmService bpm;
    static JSONObject sysConfigs;

    static long diffDateTime(Date d1, Date d2, String type){
        long rtrn = (d2.getTime()-d1.getTime());
        if(type.equals("seconds")){
            rtrn = rtrn/1000L;
        }
        if(type.equals("minutes")){
            rtrn = rtrn/(1000L*60L);
        }
        if(type.equals("hours")){
            rtrn = rtrn/(1000L*60L*60L);
        }
        if(type.equals("days")){
            rtrn = rtrn/(1000L*60L*60L*24L);
        }
        if(type.equals("weeks")){
            rtrn = rtrn/(1000L*60L*60L*24L*7L);
        }
        return rtrn;
    }
    static void loadDirectory(String path) {
        (new File(path)).mkdir();
    }
    static void loadSystemConfig() throws Exception {
        if(session == null || server == null){return;}

        IStringMatrix mtrx = server.getStringMatrix("SYS_CONFIGS", session);
        if(mtrx == null) throw new Exception("SystemConfig Global Value List not found");

        List<List<String>> rawTable = mtrx.getRawRows();
        String srvn = session.getSystem().getName().toUpperCase();
        sysConfigs = new JSONObject();

        for(List<String> line : rawTable) {
            String grpn = line.get(0);
            String name = line.get(1);
            String cval = line.get(2);

            grpn = (grpn == null ? "" : grpn);
            name = (name == null ? "" : name);
            cval = (cval == null ? "" : cval);

            if(grpn.isEmpty()){continue;}
            if(name.isEmpty()){continue;}

            if(!grpn.toUpperCase().startsWith(srvn + ".")){continue;}
            String gpnm = grpn.substring(srvn.length() + ".".length());

            JSONObject gobj = (!sysConfigs.has(gpnm) ? new JSONObject() : (JSONObject) sysConfigs.get(gpnm));
            gobj.put(name, cval);

            sysConfigs.put(gpnm, gobj);

        }
    }
    public static boolean hasDescriptor(IInformationObject object, String descName){
        IDescriptor[] descs = session.getDocumentServer().getDescriptorByName(descName, session);
        List<String> checkList = new ArrayList<>();
        for(IDescriptor ddsc : descs){
            checkList.add(ddsc.getId());
        }

        String[] descIds = new String[0];
        if(object instanceof IFolder){
            String classID = object.getClassID();
            IArchiveFolderClass folderClass = session.getDocumentServer().getArchiveFolderClass(classID , session);
            descIds = folderClass.getAssignedDescriptorIDs();
        }else if(object instanceof IDocument){
            IArchiveClass documentClass = ((IDocument) object).getArchiveClass();
            descIds = documentClass.getAssignedDescriptorIDs();
        }else if(object instanceof ITask){
            IProcessType processType = ((ITask) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }else if(object instanceof IProcessInstance){
            IProcessType processType = ((IProcessInstance) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }

        List<String> descList = Arrays.asList(descIds);
        for(String dId : descList){
            if(checkList.contains(dId)){return true;}
        }
        return false;
    }
    public static String getLiteral(String dscn){
        String rtrn = "";
        if(session == null || server == null){return rtrn;}

        IDescriptor desc = server.getDescriptorForName(session, dscn);
        if(desc != null){
            rtrn = desc.getQueryLiteral();
        }
        return rtrn;
    }
    static JSONObject getSystemConfig(String gpnm) throws Exception {
        if(sysConfigs == null){loadSystemConfig();}
        return sysConfigs.has(gpnm) ? (JSONObject) sysConfigs.get(gpnm) : new JSONObject();
    }
}
