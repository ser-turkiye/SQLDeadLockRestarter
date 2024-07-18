package ser;

import com.ser.blueline.*;
import com.ser.blueline.agents.AgentJobStatus;
import com.ser.blueline.agents.IAgentJob;
import com.ser.blueline.agents.IAgentJobQuery;
import com.ser.blueline.agents.IAgentJobQueryResult;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.IFolderConnection;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class RestartAgentJob extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    ProcessHelper processHelper;
    List<String> types = new ArrayList<>();
    @Override
    protected Object execute() {
        if (getBpm() == null)
            return resultError("Null BPM object");

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();

        try {
            processHelper = new ProcessHelper(Utils.session);

            IAgentJobQuery jobq = Utils.server.createAgentJobQuery(Utils.session);
            //jobq.setSearchStatement("");
            jobq.setSearchStatement("AGENT_JOB_STATUS IN ('EXECUTION_FAILED', 'STARTED')");

            jobq.setEndDate(new Date());
            jobq.setHitLimit(10);
            IAgentJobQueryResult jres = jobq.query();
            List<IAgentJob> list = jres.getResults();
            int jcnt = 0;
            while(!list.isEmpty()){
                for(IAgentJob ajob : list){
                    boolean rstr = false;

                    if(ajob.getAgentJobStatus() == AgentJobStatus.EXECUTION_FAILED) {
                        if (ajob.getAgentExecutionResult().toString().contains("deadlocked")) {
                            rstr = true;
                        }
                    }
                    if(ajob.getAgentJobStatus() == AgentJobStatus.STARTED) {
                        if(ajob.getCreationDate() == null
                        || Utils.diffDateTime(ajob.getCreationDate(), new Date(), "seconds") > 60){
                            rstr = true;
                        }
                    }


                    if(!rstr){continue;}

                    log.info("[" + (jcnt++) + "] -> " + ajob.getAgentDefinitionID());
                    log.info("      ---> result : " + ajob.getAgentExecutionResult());

                    try {
                        if(!ajob.restart()){throw new Exception("Job-Restart error ...");}
                    } catch(Exception ex){
                        log.info("      ---> exception : " + ex.getMessage());
                        continue;
                    }


                    log.info("      ---> completed.");
                }
                list = jres.fetchNextResults();
            }

            log.info("Tested.");
        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + e.getStackTrace() );
            return resultRestart("Exception : " + e.getMessage(),10);
        }

        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
}