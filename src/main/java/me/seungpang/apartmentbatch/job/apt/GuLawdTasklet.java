package me.seungpang.apartmentbatch.job.apt;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.seungpang.apartmentbatch.core.repository.LawdRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

@RequiredArgsConstructor
public class GuLawdTasklet implements Tasklet {

    private final LawdRepository lawdRepository;
    private List<String> guLawdCdList;
    private int itemCount;


    private static final String KEY_ITEM_COUNT = "itemCount";
    private static final String KEY_GU_LAWD_CD = "guLawdCd";
    private static final String KEY_GU_LAWD_CD_LIST = "guLawdCdList";

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
        throws Exception {
        ExecutionContext executionContext = getExecutionContext(chunkContext);
        initList(executionContext);
        initItemCount(executionContext);

        if (itemCount == 0) {
            contribution.setExitStatus(ExitStatus.COMPLETED);
            return RepeatStatus.FINISHED;
        }

        itemCount--;
        executionContext.putString(KEY_GU_LAWD_CD, guLawdCdList.get(itemCount));
        executionContext.putInt(KEY_ITEM_COUNT,itemCount);

        contribution.setExitStatus(new ExitStatus("CONTINUABLE"));
        return RepeatStatus.FINISHED;
    }

    private ExecutionContext getExecutionContext(ChunkContext chunkContext) {
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        return stepExecution.getJobExecution()
            .getExecutionContext();
    }

    private void initList(ExecutionContext executionContext) {
        if (executionContext.containsKey(KEY_GU_LAWD_CD_LIST)) {
            guLawdCdList = (List<String>) executionContext.get(KEY_GU_LAWD_CD_LIST);
        } else {
            guLawdCdList = lawdRepository.findDistinctGuLawdCd();
            executionContext.put(KEY_GU_LAWD_CD_LIST, guLawdCdList);
            executionContext.putInt(KEY_ITEM_COUNT,guLawdCdList.size());
        }
    }

    private void initItemCount(ExecutionContext executionContext) {
        if (executionContext.containsKey(KEY_ITEM_COUNT)) {
            itemCount = executionContext.getInt(KEY_ITEM_COUNT);
        } else {
            itemCount = guLawdCdList.size();
        }
    }
}
