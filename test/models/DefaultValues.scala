package models

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises, _}
import org.joda.time.LocalDate


object TradingPremisesSection {

  val address = Address("Address 1", "Address 2",None,None,"NE98 1ZZ")
  val year = 2010
  val month = 2
  val day = 1
  val date = new LocalDate(year, month, day)

  val ytp = YourTradingPremises("tradingName1", address, true, date)
  val ytp1 = YourTradingPremises("tradingName2", address, true, date)
  val ytp2 = YourTradingPremises("tradingName3", address, true, date)
  val ytp3 = YourTradingPremises("tradingName3", address, true, date)


  val businessStructure = SoleProprietor
  val testAgentName = AgentName("test")
  val testAgentCompanyName = AgentCompanyDetails("test", Some("12345678"))
  val testAgentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))

  val tradingPremisesWithHasChangedFalse = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(testAgentName),
    Some(testAgentCompanyName),
    Some(testAgentPartnership),
    Some(wdbd),
    Some(msbServices),
    false
  )

}
