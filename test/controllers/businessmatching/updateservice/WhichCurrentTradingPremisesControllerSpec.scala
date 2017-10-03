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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching.{AccountancyServices, BusinessActivity}
import models.tradingpremises.TradingPremises
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.GuiceInjectorBuilder
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._
import play.api.inject.bind
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.i18n.Messages
import services.businessmatching.BusinessMatchingService
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class WhichCurrentTradingPremisesControllerSpec extends GenericTestHelper
  with MustMatchers
  with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    implicit val request = addToken(authRequest)

    val bmService = mock[BusinessMatchingService]

    val injector = new GuiceInjectorBuilder()
      .bindings(bind[AuthConnector].to(self.authConnector))
      .bindings(bind[DataCacheConnector].to(mockCacheConnector))
      .bindings(bind[BusinessMatchingService].to(bmService))
      .build()

    lazy val controller = injector.instanceOf[WhichCurrentTradingPremisesController]

    mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises())))
  }

  "get" when {
    "called" must {
      "return the correct view" in new Fixture {
        when {
          bmService.getSubmittedBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(AccountancyServices))

        val result = controller.get()(request)

        status(result) mustBe OK

        contentAsString(result) must include(Messages("businessmatching.updateservice.whichtradingpremises.header", AccountancyServices.getMessage))
      }
    }
  }

}
