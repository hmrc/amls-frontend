package controllers.tradingpremises

import connectors.DataCacheConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import utils.AuthorisedFixture

class TradingPremisesUtilControllerSpec extends PlaySpec
  with OneServerPerSuite
  with MockitoSugar
  with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhatDoesYourBusinessDoController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
    }
  }


}
