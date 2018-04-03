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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching.BusinessMatching
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveActivitiesInformationControllerSpec extends GenericTestHelper with MockitoSugar with MustMatchers with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[BusinessMatchingService].to(mock[BusinessMatchingService]))
      .build()

    val controller = app.injector.instanceOf[RemoveActivitiesInformationController]

  }

  "RemoveActivitiesInformationController" when {
    "get is called" must {

      "display the view" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any(),any(),any())
        } thenReturn OptionT.some[Future, BusinessMatching](businessMatchingGen.sample.get)

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("updateservice.removeactivitiesinformation.title"))

      }

    }
  }

}