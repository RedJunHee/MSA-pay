package com.fastcampuspay.banking.adapter.out.service;

import org.springframework.stereotype.Component;

import com.fastcampuspay.banking.application.port.out.GetMembershipPort;
import com.fastcampuspay.banking.application.port.out.MemberShipStatus;
import com.fastcampuspay.common.CommonHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;

@Component
public class MembershipServiceAdapter implements GetMembershipPort {

	private CommonHttpClient commonHttpClient;
	private final String membershipServiceUrl;

	public MembershipServiceAdapter(CommonHttpClient commonHttpClient,
				@Value("${service.membership.url}") String membershipServiceUrl) {
		this.commonHttpClient = commonHttpClient;
		this.membershipServiceUrl = membershipServiceUrl;
	}

	@Override
	public MemberShipStatus getMemberShip(String membershipId) {

		String url = String.join("/" , membershipServiceUrl, "membership", membershipId);
		try {
			String jsonResponse = commonHttpClient.sendGetRequest(url).body();

			//json Membership
			ObjectMapper mapper = new ObjectMapper();
			Membership memberShip = mapper.readValue(jsonResponse, Membership.class);

			if(memberShip.isValid()){
				return new MemberShipStatus(memberShip.getMembershipId(),true);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		// 실제로 http call
		// http client



		return null;
	}
}
