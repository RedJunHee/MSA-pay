package com.fastcampuspay.money.application.service;

import com.fastcampuspay.common.CountDownLatchManager;
import com.fastcampuspay.common.RechargingMoneyTask;
import com.fastcampuspay.common.SubTask;
import com.fastcampuspay.common.UseCase;
import com.fastcampuspay.money.adapter.axon.command.CreateMoneyCommand;
import com.fastcampuspay.money.adapter.axon.command.IncreaseMemberMoneyCommand;
import com.fastcampuspay.money.adapter.out.persistence.MemberMoneyJpaEntity;
import com.fastcampuspay.money.adapter.out.persistence.MoneyChangingRequestMapper;
import com.fastcampuspay.money.application.port.in.CreateMemberMoneyCommand;
import com.fastcampuspay.money.application.port.in.CreateMemberMoneyUseCase;
import com.fastcampuspay.money.application.port.in.IncreaseMoneyRequestCommand;
import com.fastcampuspay.money.application.port.in.IncreaseMoneyRequestUseCase;
import com.fastcampuspay.money.application.port.out.CreateMemberMoneyPort;
import com.fastcampuspay.money.application.port.out.GetMemberMoneyPort;
import com.fastcampuspay.money.application.port.out.GetMembershipPort;
import com.fastcampuspay.money.application.port.out.IncreaseMoneyPort;
import com.fastcampuspay.money.application.port.out.SendRechargingMoneyTaskPort;
import com.fastcampuspay.money.domain.MemberMoney;
import com.fastcampuspay.money.domain.MoneyChangingRequest;

import kotlin.collections.ArrayDeque;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import org.axonframework.commandhandling.gateway.CommandGateway;

@UseCase
@RequiredArgsConstructor
@Transactional
public class IncreaseMoneyRequestService implements IncreaseMoneyRequestUseCase, CreateMemberMoneyUseCase {


    private final CountDownLatchManager countDownLatchManager;
    private final SendRechargingMoneyTaskPort sendRechargingMoneyTaskPort;
    private final GetMembershipPort getMembershipPort;
    private final IncreaseMoneyPort increaseMoneyPort;
    private final MoneyChangingRequestMapper mapper;
    private final CommandGateway commandGateway;
    private final GetMemberMoneyPort getMemberMoneyPort;
    private final CreateMemberMoneyPort createMemberMoneyPort;

    @Override
    public MoneyChangingRequest increaseMoneyRequest(IncreaseMoneyRequestCommand command) {

        // 머니의 충전.증액이라는 과정
        // 1. 고객 정보가 정상인지 확인 (멤버)
        getMembershipPort.getMemberShip(command.getTargetMembershipId());

        // 2. 고객의 연동된 계좌가 있는지, 고객의 연동된 계좌의 잔액이 충분한지도 확인 (뱅킹)

        // 3. 법인 계좌 상태도 정상인지 확인 (뱅킹)

        // 4. 증액을 위한 "기록". 요청 상태로 MoneyChangingRequest 를 생성한다. (MoneyChangingRequest)

        // 5. 펌뱅킹을 수행하고 (고객의 연동된 계좌 -> 패캠페이 법인 계좌) (뱅킹)

        // 6-1. 결과가 정상적이라면. 성공으로 MoneyChangingRequest 상태값을 변동 후에 리턴
        // 성공 시에 멤버의 MemberMoney 값 증액이 필요해요
        MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                new MemberMoney.MembershipId(command.getTargetMembershipId())
                ,command.getAmount());

        if(memberMoneyJpaEntity != null) {
            return mapper.mapToDomainEntity(increaseMoneyPort.createMoneyChangingRequest(
                            new MoneyChangingRequest.TargetMembershipId(command.getTargetMembershipId()),
                            new MoneyChangingRequest.MoneyChangingType(1),
                            new MoneyChangingRequest.ChangingMoneyAmount(command.getAmount()),
                            new MoneyChangingRequest.MoneyChangingStatus(1),
                            new MoneyChangingRequest.Uuid(UUID.randomUUID().toString())
                    )
            );
        }

        // 6-2. 결과가 실패라면, 실패라고 MoneyChangingRequest 상태값을 변동 후에 리턴
        return null;
    }
    @Override
    public MoneyChangingRequest increaseMoneyRequestAsync(IncreaseMoneyRequestCommand command){

        // Subtask 란, 각 서비스에 특정 membershipId로 Validation 하기위한 Task


        // 1. SubTask, Task
        SubTask validMemberTask = SubTask.builder()
            .subTaskName("validMemberTask : " + "멤버십 유효성 검사")
            .membershipID(command.getTargetMembershipId())
            .taskType("membership")
            .status("ready")
            .build();


        // Banking Sub task
        // a. Banking Account Validation
        SubTask validBankingAccountTask = SubTask.builder()
            .subTaskName("validBankingAccountTask : " + "뱅킹 계좌 유효성 검사")
            .membershipID(command.getTargetMembershipId())
            .taskType("banking")
            .status("ready")
            .build();


        // b. Amount Money Firmbanking => OK 가정

        List<SubTask> subTaskList = new ArrayDeque<>();
        subTaskList.add(validMemberTask);
        subTaskList.add(validBankingAccountTask);

        RechargingMoneyTask task = RechargingMoneyTask.builder()
            .taskID(UUID.randomUUID().toString())
            .taskName("Increase Money Task / 머니 충전 Task")
            .subTaskList(subTaskList)
            .moneyAmount(command.getAmount())
            .membershipID(command.getTargetMembershipId())
            .toBankName("fastcampus")
            .build();


        // 2. Kafka Cluster Produce
        // Task Produce
        sendRechargingMoneyTaskPort.sendRechargingMoneyTaskPort(task);
        countDownLatchManager.addCountDownLatch(task.getTaskID());


        // 3. Wait
		try {
			countDownLatchManager.getCountDownLatch(task.getTaskID()).await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// 3-1. task-consumer
        // 등록된 서브 태스크를 수행하고 수행 결과가 모두 OK 라면 태스크 결과를 Produce


        // 4. Task Result Consume
        String result = countDownLatchManager.getDataForKey(task.getTaskID());
        if(result.equals("success")){
            // 4-1 Consumer Ok
            MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                new MemberMoney.MembershipId(command.getTargetMembershipId())
                ,command.getAmount());

            if(memberMoneyJpaEntity != null) {
                return mapper.mapToDomainEntity(increaseMoneyPort.createMoneyChangingRequest(
                        new MoneyChangingRequest.TargetMembershipId(command.getTargetMembershipId()),
                        new MoneyChangingRequest.MoneyChangingType(1),
                        new MoneyChangingRequest.ChangingMoneyAmount(command.getAmount()),
                        new MoneyChangingRequest.MoneyChangingStatus(1),
                        new MoneyChangingRequest.Uuid(UUID.randomUUID().toString())
                    )
                );
            }
        }else{

            return null;
        }

        // 5. Consume OK, -> Logic

        return null;
    }
    @Override
    public void increaseMoneyRequestByEvent(IncreaseMoneyRequestCommand command) {
        MemberMoneyJpaEntity memberMoneyEntity = getMemberMoneyPort.getMemberMoney(new MemberMoney.MembershipId(command.getTargetMembershipId()));
        String moneyIdentifier = memberMoneyEntity.getAggregateIdentifier();

        String aggregateIdentifier = memberMoneyEntity.getAggregateIdentifier();
        //command
        commandGateway.send(IncreaseMemberMoneyCommand.builder()
                    .aggregateIdentifier(aggregateIdentifier)
                    .amount(command.getAmount())
                    .membershipId(command.getTargetMembershipId())
                    .build())
            .whenComplete((o, throwable) -> {
                if(throwable != null){
                    System.out.println("throwable = " + throwable);
                    throw new RuntimeException(throwable);
                }else {
                    // Increase money -> money increase
                    System.out.println("increaseMoney result = " + o);
                    increaseMoneyPort.increaseMoney(
                        new MemberMoney.MembershipId(command.getTargetMembershipId())
                        ,command.getAmount());
                }
            });
    }

    @Override
    public void createMemberMoney(CreateMemberMoneyCommand command) {
        commandGateway.send(CreateMoneyCommand.builder().membershipId(command.getTargetMembershipId()).build())
            // 성공 할때 까지 기다렸다가 성공할때 이벤트 받음.
            .whenComplete((Object result, Throwable throwable) -> {
                if (throwable == null) {
                    System.out.println("Create Money Aggregate ID:" + result.toString());
                    createMemberMoneyPort.createMemberMoney(
                        new MemberMoney.MembershipId(command.getTargetMembershipId())
                        , new MemberMoney.MoneyAggregateIdentifier(result.toString()));
                } else {
                    throwable.printStackTrace();
                    System.out.println("error : " + throwable.getMessage());
                }
            });
    }
}
