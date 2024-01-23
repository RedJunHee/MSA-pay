package com.fastcampuspay.money.adapter.axon.command;


import javax.validation.constraints.NotNull;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.fastcampuspay.common.SelfValidating;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class IncreaseMemberMoneyCommand extends SelfValidating<IncreaseMemberMoneyCommand> {

	@NotNull
	@TargetAggregateIdentifier
	private String aggregateIdentifier;

	@NotNull
	private String membershipId;

	@NotNull
	private final int amount;


}
