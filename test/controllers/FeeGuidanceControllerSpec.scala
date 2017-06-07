/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businessmatching._
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.{AgentCompanyDetails, TradingPremises}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.PrivateMethodTester
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}

class FeeGuidanceControllerSpec extends GenericTestHelper with MockitoSugar with PrivateMethodTester with ServicesConfig {

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)

    val mockDataCacheConnector = mock[DataCacheConnector]

    val mockCacheMap = mock[CacheMap]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    val controller = new FeeGuidanceController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector
    )

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .build()

    val nonEmptyTradingPremises = TradingPremises(agentCompanyDetails = Some(AgentCompanyDetails("test", Some("12345678"))))

    val submissionFee = getInt(s"$rootServices.amounts.registration")
    val premisesFee = getInt(s"$rootServices.amounts.premises")
    val peopleFee = getInt(s"$rootServices.amounts.people")

    val breakdownRows = Seq(
      BreakdownRow(Messages("confirmation.submission"), 1, Currency(submissionFee), Currency(submissionFee)),
      BreakdownRow(Messages("summary.responsiblepeople"), 3, Currency(peopleFee), Currency(peopleFee * 3)),
      BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
    )

    when(mockDataCacheConnector.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

  }

  "FeeGuidanceController" when {

    "get is called" must {

      "show fee guidance page" in new Fixture {

        when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
          .thenReturn(None)

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any()))
          .thenReturn(None)

        val result = controller.get()(request)
        status(result) must be(OK)

      }

    }

    "getBreakdownRows is called" must {

      "return all breakdownRows" when {

        "the business type contains msb" when {

          "trading premises and responsible people (not fit&proper) are present and not already registered" in new Fixture {

            val aboutthebusiness = AboutTheBusiness(
              previouslyRegistered = Some(PreviouslyRegisteredNo)
            )

            val tradingpremises = Seq(
              nonEmptyTradingPremises,
              nonEmptyTradingPremises
            )

            val responsiblepeople = Seq(
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

            val businessmatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
            )

            when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
              .thenReturn(Some(tradingpremises))

            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any()))
              .thenReturn(Some(responsiblepeople))

            when(mockCacheMap.getEntry[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any()))
              .thenReturn(Some(aboutthebusiness))

            when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
              .thenReturn(Some(businessmatching))

            val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)

            val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

            await(result) must be(breakdownRows)

          }

        }

        "the business type contains tcsp" when {

          "trading premises and responsible people (not fit&proper) are present and not already registered" in new Fixture {

            val aboutthebusiness = AboutTheBusiness(
              previouslyRegistered = Some(PreviouslyRegisteredNo)
            )

            val tradingpremises = Seq(
              nonEmptyTradingPremises,
              nonEmptyTradingPremises
            )

            val responsiblepeople = Seq(
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

            val businessmatching = BusinessMatching(
              activities = Some(BusinessActivities(Set(TrustAndCompanyServices)))
            )

            when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
              .thenReturn(Some(tradingpremises))

            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any()))
              .thenReturn(Some(responsiblepeople))

            when(mockCacheMap.getEntry[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any()))
              .thenReturn(Some(aboutthebusiness))

            when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
              .thenReturn(Some(businessmatching))

            val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)

            val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

            await(result) must be(breakdownRows)

          }

        }


      }

      "return a breakdown showing no submission fee" when {

        "already registered for MLR" in new Fixture {

          override val breakdownRows = Seq(
            BreakdownRow(Messages("summary.responsiblepeople"), 3, Currency(peopleFee), Currency(peopleFee * 3)),
            BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
          )

          val tradingpremises = Seq(
            nonEmptyTradingPremises,
            nonEmptyTradingPremises
          )

          val responsiblepeople = Seq(
            ResponsiblePeople(),
            ResponsiblePeople(),
            ResponsiblePeople()
          )

          val aboutthebusiness = AboutTheBusiness(
            previouslyRegistered = Some(PreviouslyRegisteredYes("regNo"))
          )

          val businessmatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
            .thenReturn(Some(tradingpremises))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any()))
            .thenReturn(Some(responsiblepeople))

          when(mockCacheMap.getEntry[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any()))
            .thenReturn(Some(aboutthebusiness))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(businessmatching))

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

          val aboutthebusiness = AboutTheBusiness(
            previouslyRegistered = Some(PreviouslyRegisteredNo)
          )

          val tradingpremises = Seq(
            nonEmptyTradingPremises,
            nonEmptyTradingPremises
          )

          val responsiblepeople = Seq(
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

          val businessmatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
            .thenReturn(Some(tradingpremises))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any()))
            .thenReturn(Some(responsiblepeople))

          when(mockCacheMap.getEntry[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any()))
            .thenReturn(Some(aboutthebusiness))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(businessmatching))

          val privateGetBreakdownRows = PrivateMethod[Future[Seq[BreakdownRow]]]('getBreakdownRows)

          val result = controller invokePrivate privateGetBreakdownRows(HeaderCarrier(), mock[AuthContext])

          await(result) must be(breakdownRows)


        }

        "the business type does not include msb or tcsp" in new Fixture {

          override val breakdownRows = Seq(
            BreakdownRow(Messages("confirmation.submission"), 1, Currency(submissionFee), Currency(submissionFee)),
            BreakdownRow(Messages("summary.tradingpremises"), 2, Currency(premisesFee), Currency(premisesFee * 2))
          )

          val aboutthebusiness = AboutTheBusiness(
            previouslyRegistered = Some(PreviouslyRegisteredNo)
          )

          val tradingpremises = Seq(
            nonEmptyTradingPremises,
            nonEmptyTradingPremises
          )

          val responsiblepeople = Seq(
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

          val businessmatching = BusinessMatching(
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
            .thenReturn(Some(tradingpremises))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any()))
            .thenReturn(Some(responsiblepeople))

          when(mockCacheMap.getEntry[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any()))
            .thenReturn(Some(aboutthebusiness))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(Some(businessmatching))

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
