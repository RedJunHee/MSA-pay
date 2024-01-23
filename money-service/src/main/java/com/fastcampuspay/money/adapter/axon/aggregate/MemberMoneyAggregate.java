package com.fastcampuspay.money.adapter.axon.aggregate;



import lombok.Data;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import javax.validation.constraints.NotNull;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import com.fastcampuspay.money.adapter.axon.command.CreateMoneyCommand;
import com.fastcampuspay.money.adapter.axon.command.IncreaseMemberMoneyCommand;
import com.fastcampuspay.money.adapter.axon.event.IncreaseMemberMoneyEvent;
import com.fastcampuspay.money.adapter.axon.event.MemberMoneyCreateEvent;

@Aggregate()
@Data
public class MemberMoneyAggregate {
	@AggregateIdentifier
	private String id;

	private Long membershipId;

	private int balance;

	@CommandHandler
	public MemberMoneyAggregate(@NotNull CreateMoneyCommand command) {
		System.out.println("CreateMoneyCommand Handler");
		// store event
		apply(new MemberMoneyCreateEvent(command.getMembershipId()));
	}
	@EventSourcingHandler
	public void on(MemberMoneyCreateEvent event) {
		System.out.println("MemberMoneyCreateEvent Sourcing Handler");
		id = UUID.randomUUID().toString();
		membershipId = Long.parseLong(event.getMembershipId());
		balance = 0;
	}


	@CommandHandler
	public String handle(@NotNull IncreaseMemberMoneyCommand command) {
		System.out.println("IncreaseMoneyRequestEventCommand Handler");
		id = command.getAggregateIdentifier();

		// store event
		apply(new IncreaseMemberMoneyEvent(id, command.getMembershipId(), command.getAmount()));
		return id;
	}

	@EventSourcingHandler
	public void on(IncreaseMemberMoneyEvent event){
		System.out.println("IncreaseMemberMoneyEvent Sourcing Handler");
		id = UUID.randomUUID().toString();
		membershipId = Long.parseLong(event.getTargetMembershipId());
		balance = event.getAmount();

	}

	public MemberMoneyAggregate() {
		// Required by Axon to construct an empty instance to initiate Event Sourcing.
	}

}