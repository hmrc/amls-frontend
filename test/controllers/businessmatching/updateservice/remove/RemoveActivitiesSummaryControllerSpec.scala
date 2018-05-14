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

import models.businessmatching.MoneyServiceBusiness
import models.flowmanagement.RemoveServiceFlowModel
import org.jsoup.Jsoup
import play.api.i18n.Messages
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.test.Helpers._
import views.TitleValidator

class RemoveActivitiesSummaryControllerSpec extends AmlsSpec with TitleValidator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val controller = new RemoveActivitiesSummaryController(
      self.authConnector,
      mockCacheConnector
    )
  }

  "A successful result is returned" when {
    "the user visits the page" when {
      "editing data that has no date of change" in new Fixture {
        mockCacheFetch(Some(RemoveServiceFlowModel(Some(Set(MoneyServiceBusiness)))))

        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        validateTitle(s"${Messages("title.cya")} - ${Messages("summary.updateinformation")}")(implicitly, doc)
        doc.getElementsByTag("h1").text must include(Messages("title.cya"))
        doc.getElementsByClass("check-your-answers").text must include(MoneyServiceBusiness.getMessage)
      }
    }
  }

  "A failure is returned" when {
    "the user visits the page" when {
      "there is no data to show" in new Fixture {
        mockCacheFetch(None)

        val result = controller.get()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
