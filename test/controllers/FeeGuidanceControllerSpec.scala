/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.ApplicationConfig
import connectors.DataCacheConnector
import generators.ResponsiblePersonGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businessmatching._
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import org.mockito.Matchers.{eq => eqTo}
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

import scala.concurrent.Future

class FeeGuidanceControllerSpec extends AmlsSpec
  with PrivateMethodTester
  with ServicesConfig
  with ResponsiblePersonGenerator
  with TradingPremisesGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> false)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[FeeGuidanceController]

    val nonEmptyTradingPremises = tradingPremisesGen.sample.get

    val submissionFee = ApplicationConfig.regFee
    val premisesFee = ApplicationConfig.premisesFee
    val peopleFee = ApplicationConfig.peopleFee

    val breakdownRows = Seq(
      BreakdownRow(Messages("confirmation.submission"), 1, Currency(submissionFee), Currency(submissionFee)),
      BreakdownRow(Messages("summary.responsiblepeople"), 3, Currency(peopleFee), Currency(peopleFee * 3)),
      BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
    )
  }

  "FeeGuidanceController" when {

    "get is called" must {

      "show fee guidance page" in new Fixture {

        override val builder = defaultBuilder.configure("microservice.services.feature-toggle.show-fees" -> true)

        mockCacheGetEntry[Seq[TradingPremises]](None, TradingPremises.key)
        mockCacheGetEntry[Seq[ResponsiblePeople]](None, ResponsiblePeople.key)

        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "return notFound if show-fees toggle is off" in new Fixture {
        mockCacheGetEntry[Seq[TradingPremises]](None, TradingPremises.key)
        mockCacheGetEntry[Seq[ResponsiblePeople]](None, ResponsiblePeople.key)

        override val builder = defaultBuilder.configure("microservice.services.feature-toggle.show-fees" -> false)

        val result = controller.get()(request)

        status(result) must be(NOT_FOUND)

      }
    }

    "getBreakdownRows is called" must {

      "return all breakdownRows" when {

        "the business type contains msb" when {

          "trading premises and responsible people (not fit&proper) are present and not already registered" in new Fixture {

            val aboutTheBusiness = AboutTheBusiness(
              previouslyRegistered = Some(PreviouslyRegisteredNo)
            )

            val tradingPremises = Seq(
              nonEmptyTradingPremises,
              nonEmptyTradingPremises
            )

            val responsiblePeople = Seq(
              ResponsiblePeople(
                hasAlreadyPassedFitAndProper = Some(false)
              ),
              ResponsiblePeople(
                hasAlreadyPassedFitAndProper = Some(false)
              ),
              ResponsiblePeople(
                hasAlreadyPassedFitAndProper = Some(false)
              )
            )

            val businessMatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
            )

            mockCacheGetEntry(Some(tradingPremises), TradingPremises.key)
            mockCacheGetEntry(Some(responsiblePeople), ResponsiblePeople.key)
            mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
            mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

            val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)

            val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

            await(result) must be(breakdownRows)

          }

        }

        "the business type contains tcsp" when {

          "trading premises and responsible people (not fit&proper) are present and not already registered" in new Fixture {

            val aboutTheBusiness = AboutTheBusiness(
              previouslyRegistered = Some(PreviouslyRegisteredNo)
            )

            val tradingPremises = Seq(
              nonEmptyTradingPremises,
              nonEmptyTradingPremises
            )

            val responsiblePeople = Seq(
              ResponsiblePeople(
                hasAlreadyPassedFitAndProper = Some(false)
              ),
              ResponsiblePeople(
                hasAlreadyPassedFitAndProper = Some(false)
              ),
              ResponsiblePeople(
                hasAlreadyPassedFitAndProper = Some(false)
              )
            )

            val businessMatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(TrustAndCompanyServices)))
            )

            mockCacheGetEntry(Some(tradingPremises), TradingPremises.key)
            mockCacheGetEntry(Some(responsiblePeople), ResponsiblePeople.key)
            mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
            mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

            val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)
            val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

            await(result) must be(breakdownRows)
          }
        }
      }

      "filter out empty responsible people" in new Fixture {
        val people = Seq(
          responsiblePersonGen.sample.get,
          ResponsiblePeople()
        )

        val aboutTheBusiness = AboutTheBusiness(
          previouslyRegistered = Some(PreviouslyRegisteredYes("regNo"))
        )

        val businessMatching = BusinessMatching(
          activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
        )

        override val breakdownRows = Seq(
          BreakdownRow(Messages("summary.responsiblepeople"), 1, Currency(peopleFee), Currency(peopleFee))
        )

        mockCacheGetEntry(Some(people), ResponsiblePeople.key)
        mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
        mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)
        mockCacheGetEntry(Some(Seq.empty[TradingPremises]), TradingPremises.key)

        val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)
        val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

        await(result) must be(breakdownRows)
      }

      "filter out empty trading premises" in new Fixture {
        val people = Seq(
          responsiblePersonGen.sample.get
        )

        val aboutTheBusiness = AboutTheBusiness(
          previouslyRegistered = Some(PreviouslyRegisteredYes("regNo"))
        )

        val businessMatching = BusinessMatching(
          activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
        )

        val tradingPremises = Seq(
          tradingPremisesGen.sample.get,
          TradingPremises()
        )

        override val breakdownRows = Seq(
          BreakdownRow(Messages("summary.responsiblepeople"), 1, Currency(peopleFee), Currency(peopleFee)),
          BreakdownRow(Messages("summary.tradingpremises"), 1, Currency(premisesFee), Currency(premisesFee))
        )

        mockCacheGetEntry(Some(people), ResponsiblePeople.key)
        mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
        mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)
        mockCacheGetEntry(Some(tradingPremises), TradingPremises.key)

        val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)
        val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

        await(result) must be(breakdownRows)
      }

      "return a breakdown showing no submission fee" when {

        "already registered for MLR" in new Fixture {

          override val breakdownRows = Seq(
            BreakdownRow(Messages("summary.responsiblepeople"), 3, Currency(peopleFee), Currency(peopleFee * 3)),
            BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
          )

          val tradingPremises = Gen.listOfN(2, tradingPremisesGen).sample.get

          val responsiblePeople = responsiblePeopleGen(3).sample.get

          val aboutTheBusiness = AboutTheBusiness(
            previouslyRegistered = Some(PreviouslyRegisteredYes("regNo"))
          )

          val businessMatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
          )

          mockCacheGetEntry(Some(tradingPremises), TradingPremises.key)
          mockCacheGetEntry(Some(responsiblePeople), ResponsiblePeople.key)
          mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
          mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

          val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)

          val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

          await(result) must be(breakdownRows)

        }

      }

      "return a breakdown showing no responsible people" when {

        "all responsible people are fit&proper" in new Fixture {

          override val breakdownRows = Seq(
            BreakdownRow(Messages("confirmation.submission"), 1, Currency(submissionFee), Currency(submissionFee)),
            BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
          )

          val aboutTheBusiness = AboutTheBusiness(
            previouslyRegistered = Some(PreviouslyRegisteredNo)
          )

          val tradingPremises = Seq(
            nonEmptyTradingPremises,
            nonEmptyTradingPremises
          )

          val responsiblePeople = Seq(
            ResponsiblePeople(
              hasAlreadyPassedFitAndProper = Some(true)
            ),
            ResponsiblePeople(
              hasAlreadyPassedFitAndProper = Some(true)
            ),
            ResponsiblePeople(
              hasAlreadyPassedFitAndProper = Some(true)
            )
          )

          val businessMatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
          )

          mockCacheGetEntry(Some(tradingPremises), TradingPremises.key)
          mockCacheGetEntry(Some(responsiblePeople), ResponsiblePeople.key)
          mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
          mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

          val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)
          val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

          await(result) must be(breakdownRows)
        }

        "the business type does not include msb or tcsp" in new Fixture {

          override val breakdownRows = Seq(
            BreakdownRow(Messages("confirmation.submission"), 1, Currency(submissionFee), Currency(submissionFee)),
            BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
          )

          val aboutTheBusiness = AboutTheBusiness(
            previouslyRegistered = Some(PreviouslyRegisteredNo)
          )

          val tradingPremiseses = Seq(
            nonEmptyTradingPremises,
            nonEmptyTradingPremises
          )

          val responsiblePeople = Seq(
            ResponsiblePeople(
              hasAlreadyPassedFitAndProper = Some(false)
            ),
            ResponsiblePeople(
              hasAlreadyPassedFitAndProper = Some(false)
            ),
            ResponsiblePeople(
              hasAlreadyPassedFitAndProper = Some(false)
            )
          )

          val businessMatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          mockCacheGetEntry(Some(tradingPremiseses), TradingPremises.key)
          mockCacheGetEntry(Some(responsiblePeople), ResponsiblePeople.key)
          mockCacheGetEntry(Some(aboutTheBusiness), AboutTheBusiness.key)
          mockCacheGetEntry(Some(businessMatching), BusinessMatching.key)

          val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)

          val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

          await(result) must be(breakdownRows)

        }

      }

    }

    "getTotal is called" must {

      "return total from all fees in breakdown rows" in new Fixture {

        val privateGetTotal = PrivateMethod[Int]('getTotal)

        val result = controller invokePrivate privateGetTotal(breakdownRows)

        result must be(100 + (peopleFee * 3) + (premisesFee * 2))

      }

    }

  }

}
