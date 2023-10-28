package com.example.group11.controller.qa;


import com.example.group11.commons.utils.JWTUtil;
import com.example.group11.commons.utils.RestResult;
import com.example.group11.entity.sql.Question;
import com.example.group11.model.QuestionModel;
import com.example.group11.service.qa.QAService;
import com.example.group11.service.transaction.TransactionService;
import com.example.group11.vo.QuestionDetailVO;
import com.example.group11.vo.QuestionVO;
import com.example.group11.vo.RespondentDetailVO;
import com.example.group11.vo.UserVO;
import com.example.group11.vo.query.QuestionQueryVO;
import com.example.group11.vo.query.RespondentQueryVO;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/question")
public class QuestionController {

    @DubboReference(version = "1.0.0", interfaceClass = com.example.group11.service.qa.QAService.class)
    QAService qaService;

    @DubboReference(version = "1.0.0", interfaceClass = com.example.group11.service.transaction.TransactionService.class, check = false)
    TransactionService transactionService;

    @GetMapping("/all/{userId}")
    @ApiOperation(notes = "查询用户全部问题(已测试)", value = "查询用户全部问题", tags = "问题管理")
    public RestResult<Page<QuestionModel>> queryAllQuestionList(@PathVariable Long userId, Integer pageNo, Integer pageSize,
                                                                HttpServletRequest httpServletRequest) {

        QuestionQueryVO questionQueryVO = new QuestionQueryVO();
        questionQueryVO.setAskerId(userId);
        questionQueryVO.setPageNo(pageNo);
        questionQueryVO.setPageSize(pageSize);

        Page<QuestionModel> questions = qaService.getUserQuestionsByPage(questionQueryVO);
        return RestResult.ok(questions);
    }

//    @GetMapping("/all/respondent-list")
//    @ApiOperation(notes = "根据多条件查询全部答主的分页列表", value = "根据多条件查询全部答主的分页列表", tags = "问题管理")
//    public RestResult<Page<UserVO>> queryAllRespondentQuestionList(RespondentQueryVO params, HttpServletRequest httpServletRequest) {
//
//        return RestResult.ok();
//    }

    @GetMapping("/hot/question-list")
    @ApiOperation(notes = "首页热门回答列表（还没有想好按照哪种方式判断热门回答）", value = "首页热门回答列表", tags = "问题管理")
    public RestResult<List<QuestionVO>> queryHotQuestionList() {
        return RestResult.ok();
    }

    @GetMapping("/my")
    @ApiOperation(notes = "我的问题(已测试)", value = "我的问题", tags = "问题管理")
    public RestResult<Page<QuestionModel>> queryMyQuestionList(Integer pageNo, Integer pageSize,
                                                               HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);
//        根据当前用户的userId查询问题
        try {
            QuestionQueryVO questionQueryVO = new QuestionQueryVO();
            questionQueryVO.setAskerId(userId);
            questionQueryVO.setPageNo(pageNo);
            questionQueryVO.setPageSize(pageSize);


            Page<QuestionModel> page = qaService.getUserQuestionsByPage(questionQueryVO);
            return RestResult.ok(page);
        } catch (Exception e) {
            return RestResult.fail(e.getMessage());
        }
    }

    @GetMapping("/my/answer-list")
    @ApiOperation(notes = "查看我的回答的问题(已测试)", value = "查看我的回答的问题", tags = "问题管理")
    public RestResult<Page<Question>> queryMyAnswerList(Integer pageNo, Integer pageSize, HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);

        return RestResult.ok(qaService.getResponderAnsweredQuestionsByPage(userId, pageNo, pageSize));
    }

    @GetMapping("/my/waiting_answer-list")
    @ApiOperation(notes = "查看待回答的问题（已测试）", value = "查看待回答的问题", tags = "问题管理")
    public RestResult<Page<Question>> queryMyWaitingAnswerList(Integer pageNo, Integer pageSize, HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);

        userId = 3L;

        return RestResult.ok(qaService.checkUnansweredQuestionByResponderIdByPage(userId, pageNo, pageSize));
    }

    @GetMapping("/eavesdropping")
    @ApiOperation(notes = "查看我的偷听记录(已测试)", value = "查看我的偷听记录", tags = "问题管理")
    public RestResult<Page<Question>> queryMyEavesdroppingQuestion(Integer pageNo, Integer pageSize, HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);

        return RestResult.ok(qaService.checkEavesdropQuestionByUserIdByPage(userId, pageNo, pageSize));
    }

    @GetMapping("/eavesdropping/{questionId}")
//    若该回答还有音频版本，则调用该接口后，再调用统一下载接口对应音频文件实现在线播放
    @ApiOperation(notes = "偷听问题(已测试)",
            value = "偷听问题", tags = "问题管理")
    public RestResult<Boolean> eavesdroppingQuestion(@PathVariable Integer questionId, HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);

        boolean paid = qaService.checkUserEavesdropQuestion(userId, questionId);

        if (paid) {
            return RestResult.ok(paid);
        } else {
            // 说明该用户没有购买此问题
            try {
                // 扣钱，变更问题对该用户状态(不可见->可见)，返回回答内容
//        transactionService.payForEavesdroppingQuestion(userId, questionId);
                Integer transactionId = 0;
                // 若该回答还有音频版本，则调用该接口后，再调用统一下载接口对应音频文件实现在线播放
                qaService.eavesdropQuestionById(userId, questionId, transactionId);
            } catch (Exception e) {
                return RestResult.fail(e.getMessage());
            }
            return RestResult.ok(true);
        }
    }



    @PostMapping("/{responderId}/{content}/{reward:.+}")
    @ApiOperation(notes = "提问问题，首先创建问题，然后再进行支付（已测试）", value = "提问问题", tags = "问题管理")
//    还没有支付
    public RestResult<Map<String, Question>> askQuestion(@PathVariable Long responderId, @PathVariable String content,
                                                         @PathVariable Double reward, HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);

//       所有身份的用户都可以发布问题，但是提问者回答者id不能相同
        if (!userId.equals(responderId)) {
            // paid默认为false
            // answerId默认为-1, 表示未回答，回答时更新
            log.info(String.valueOf(reward));
            BigDecimal price = BigDecimal.valueOf(reward);
            log.info(String.valueOf(price));
            Question question = new Question(userId, responderId, content, price, false, -1, 0, 0);
            log.info(String.valueOf(question.getReward()));
            try {
                qaService.createQuestion(question);
            } catch (Exception e) {
                return RestResult.fail(e.getMessage());
            }
            Map<String, Question> questionMap = new HashMap<>();
            questionMap.put("question", question);
            return RestResult.ok(questionMap);
        } else {
            return RestResult.fail("writer cannot write question to himself");
        }
    }

    @DeleteMapping("/{questionId}")
    @ApiOperation(notes = "删除问题（已测试）", value = "删除问题", tags = "问题管理")
    public RestResult<Boolean> deleteQuestion(@PathVariable Integer questionId, HttpServletRequest httpServletRequest) {
        // 获取当前用户id
        String token = JWTUtil.getToken(httpServletRequest);
        Long userId = JWTUtil.getUserId(token);
        boolean deleted;
//        删除问题
        try {
            deleted = qaService.deleteQuestion(userId, questionId);
        } catch (Exception e) {
            return RestResult.fail(e.getMessage());
        }
        return RestResult.ok(deleted);
    }
}
