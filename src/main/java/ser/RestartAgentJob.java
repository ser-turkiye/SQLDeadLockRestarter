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
import org.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class RestartAgentJob extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    ProcessHelper processHelper;
    JSONObject config;
    Connection conn;
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
            jobq.setSearchStatement("AGENT_JOB_STATUS = 'EXECUTION_FAILED'");
            //jobq.setSearchStatement("");
            //jobq.setStartDate(Utils.todayMidnight());
            jobq.setEndDate(new Date());
            jobq.setHitLimit(10);
            IAgentJobQueryResult jres = jobq.query();
            List<IAgentJob> list = jres.getResults();
            int jcnt = 0;
            while(list.size() > 0){
                for(IAgentJob ajob : list){
                    //if(ajob.getAgentJobStatus() != AgentJobStatus.EXECUTION_FAILED){continue;}
                    System.out.println("[" + (jcnt++) + "] -> " + ajob.getAgentDefinitionID());
                    System.out.println("      ---> " + ajob.getAgentExecutionResult());
                    ajob.restart();
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