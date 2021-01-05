/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
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
import uk.gov.hmrc.http.cache.client.CacheMap
import utils._
import views.TitleValidator
import views.html.businessmatching.updateservice.remove.remove_activities_summary


import scala.concurrent.Future

class RemoveBusinessTypesSummaryControllerSpec extends AmlsSpec with TitleValidator {

  trait Fixture extends DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val removeServiceHelper = mock[RemoveBusinessTypeHelper]
    val router = createRouter[RemoveBusinessTypeFlowModel]
    lazy val view = app.injector.instanceOf[remove_activities_summary]
    val controller = new RemoveBusinessTypesSummaryController(
      SuccessfulAuthAction, ds = commonDependencies,
      mockCacheConnector,
      removeServiceHelper,
      router,
      cc = mockMcc,
      remove_activities_summary = view
    )
  }

  "A successful result is returned" when {
    "the user visits the page" which {
      "contains the correct content" in new Fixture {
        // scalastyle:off magic.number
        val now = new LocalDate(2014, 1, 1)
        mockCacheFetch(Some(RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)), Some(DateOfChange(now)))))

        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        validateTitle(s"${Messages("title.cya")} - ${Messages("summary.updateinformation")}")(implicitly, doc)
        doc.getElementsByTag("h1").text must include(Messages("title.cya"))
        doc.getElementsByClass("cya-summary-list").text must include(MoneyServiceBusiness.getMessage())
        doc.getElementsByClass("cya-summary-list").text must include(DateHelper.formatDate(now))
      }
    }

    "the user presses the button" which {
      "updates the application with the new data" in new Fixture {

        val flowModel = RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)))

        mockCacheFetch(Some(flowModel), Some(RemoveBusinessTypeFlowModel.key))

        when(removeServiceHelper.removeBusinessMatchingBusinessTypes(any(), eqTo(flowModel))(any(), any()))
          .thenReturn(OptionT.some[Future, BusinessMatching](mock[BusinessMatching]))

        when(removeServiceHelper.removeFitAndProper(any(), eqTo(flowModel))(any(), any()))
          .thenReturn(OptionT.some[Future, Seq[ResponsiblePerson]](Seq.empty))

        when(removeServiceHelper.removeTradingPremisesBusinessTypes(any(), eqTo(flowModel))(any(), any()))
          .thenReturn(OptionT.some[Future, Seq[TradingPremises]](Seq.empty))

        when(removeServiceHelper.removeSectionData(any(), eqTo(flowModel))(any(), any()))
          .thenReturn(OptionT.some[Future, Seq[CacheMap]](Seq.empty))

        when(removeServiceHelper.removeFlowData(any())(any(), any()))
          .thenReturn(OptionT.some[Future, RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel()))

        val result = controller.post()(requestWithUrlEncodedBody("" -> ""))

        status(result) mustBe SEE_OTHER

        router.verify("internalId", RemoveBusinessTypesSummaryPageId, flowModel)
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
