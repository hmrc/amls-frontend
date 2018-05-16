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
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import models.DateOfChange
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness}
import models.flowmanagement.{RemoveBusinessTypeFlowModel, RemoveBusinessTypesSummaryPageId}
import models.responsiblepeople.ResponsiblePerson
import models.tradingpremises.TradingPremises
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import org.mockito.Matchers.{any, eq => eqTo}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DateHelper, DependencyMocks}
import views.TitleValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveBusinessTypeSummaryControllerSpec extends AmlsSpec with TitleValidator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val removeServiceHelper = mock[RemoveBusinessTypeHelper]
    val router = createRouter[RemoveBusinessTypeFlowModel]

    val controller = new RemoveBusinessTypeSummaryController(
      self.authConnector,
      mockCacheConnector,
      removeServiceHelper,
      router
    )
  }

  "A successful result is returned" when {
    "the user visits the page" which {
      "contains the correct content" in new Fixture {
        val now = new LocalDate(2014, 1, 1)
        mockCacheFetch(Some(RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)), Some(DateOfChange(now)))))

        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        validateTitle(s"${Messages("title.cya")} - ${Messages("summary.updateinformation")}")(implicitly, doc)
        doc.getElementsByTag("h1").text must include(Messages("title.cya"))
        doc.getElementsByClass("check-your-answers").text must include(MoneyServiceBusiness.getMessage)
        doc.getElementsByClass("check-your-answers").text must include(DateHelper.formatDate(now))
      }
    }

    "the user presses the button" which {
      "updates the application with the new data" in new Fixture {

        val flowModel = RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)))

        mockCacheFetch(Some(flowModel), Some(RemoveBusinessTypeFlowModel.key))

        when(removeServiceHelper.removeBusinessMatchingBusinessTypes(eqTo(flowModel))(any(), any(), any()))
          .thenReturn(OptionT.some[Future, BusinessMatching](mock[BusinessMatching]))

        when(removeServiceHelper.removeFitAndProper(eqTo(flowModel))(any(), any(), any()))
          .thenReturn(OptionT.some[Future, Seq[ResponsiblePerson]](Seq.empty))

        when(removeServiceHelper.removeTradingPremisesBusinessTypes(eqTo(flowModel))(any(), any(), any()))
          .thenReturn(OptionT.some[Future, Seq[TradingPremises]](Seq.empty))

        val result = controller.post()(request.withFormUrlEncodedBody())

        status(result) mustBe SEE_OTHER

        router.verify(RemoveBusinessTypesSummaryPageId, flowModel)
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
