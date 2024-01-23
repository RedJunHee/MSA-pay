package com.fastcampuspay.money.adapter.out.persistence;

import com.fastcampuspay.common.PersistenceAdapter;
import com.fastcampuspay.money.application.port.out.CreateMemberMoneyPort;
import com.fastcampuspay.money.application.port.out.GetMemberMoneyPort;
import com.fastcampuspay.money.application.port.out.IncreaseMoneyPort;
import com.fastcampuspay.money.domain.MemberMoney;
import com.fastcampuspay.money.domain.MoneyChangingRequest;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@PersistenceAdapter
@RequiredArgsConstructor
public class MoneyChangingRequestPersistenceAdapter implements IncreaseMoneyPort, GetMemberMoneyPort, CreateMemberMoneyPort {

    private final SpringDataMoneyChangingRequestRepository moneyChangingRequestRepository;

    private final SpringDataMemberMoneyRepository memberMoneyRepository;
    @Override
    public MoneyChangingRequestJpaEntity createMoneyChangingRequest(MoneyChangingRequest.TargetMembershipId targetMembershipId, MoneyChangingRequest.MoneyChangingType moneyChangingType, MoneyChangingRequest.ChangingMoneyAmount changingMoneyAmount, MoneyChangingRequest.MoneyChangingStatus moneyChangingStatus, MoneyChangingRequest.Uuid uuid) {
        return moneyChangingRequestRepository.save(
                new MoneyChangingRequestJpaEntity(
                        targetMembershipId.getTargetMembershipId(),
                        moneyChangingType.getMoneyChangingType(),
                        changingMoneyAmount.getChangingMoneyAmount(),
                        new Timestamp(System.currentTimeMillis()), // TODO: 2021-08-17 11:00:00
                        moneyChangingStatus.getChangingMoneyStatus(),
                        UUID.randomUUID()
                )
        );
    }

    @Override
    public MemberMoneyJpaEntity increaseMoney(MemberMoney.MembershipId memberId, int increaseMoneyAmount) {
        MemberMoneyJpaEntity entity;

        List<MemberMoneyJpaEntity> entityList =  memberMoneyRepository.findByMembershipId(Long.parseLong(memberId.getMembershipId()));
        entity = entityList.get(0);

        entity.setBalance(entity.getBalance() + increaseMoneyAmount);
        return  memberMoneyRepository.save(entity);
    }

    @Override
    public MemberMoneyJpaEntity getMemberMoney(MemberMoney.MembershipId memberId) {
        MemberMoneyJpaEntity entity;

        List<MemberMoneyJpaEntity> entityList =  memberMoneyRepository.findByMembershipId(Long.parseLong(memberId.getMembershipId()));
        entity = entityList.get(0);

        return  memberMoneyRepository.save(entity);
    }

    @Override
    public void createMemberMoney(MemberMoney.MembershipId memberId, MemberMoney.MoneyAggregateIdentifier aggregateIdentifier) {
        MemberMoneyJpaEntity entity = new MemberMoneyJpaEntity(
            Long.parseLong(memberId.getMembershipId()),
            0, aggregateIdentifier.getAggregateIdentifier()
        );
        memberMoneyRepository.save(entity);
    }

}
