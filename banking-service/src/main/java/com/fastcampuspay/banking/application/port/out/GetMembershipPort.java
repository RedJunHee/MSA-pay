package com.fastcampuspay.banking.application.port.out;

import com.fastcampuspay.banking.adapter.out.service.Membership;

public interface GetMembershipPort {
	public MemberShipStatus getMemberShip(String membershipId);

}
