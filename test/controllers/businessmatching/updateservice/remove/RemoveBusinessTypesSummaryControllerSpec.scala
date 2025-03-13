/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import models.DateOfChange
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import models.businessmatching.BusinessMatching
import models.flowmanagement.{RemoveBusinessTypeFlowModel, RemoveBusinessTypesSummaryPageId}
import models.responsiblepeople.ResponsiblePerson
import models.tradingpremises.TradingPremises
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers._
import services.cache.Cache
import utils._
import views.TitleValidator
import views.html.businessmatching.updateservice.remove.RemoveActivitiesSummaryView

import java.time.LocalDate
import scala.concurrent.Future

class RemoveBusinessTypesSummaryControllerSpec extends AmlsSpec with TitleValidator {

  trait Fixture extends DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val removeServiceHelper = mock[RemoveBusinessTypeHelper]
    val router              = createRouter[RemoveBusinessTypeFlowModel]
    lazy val view           = app.injector.instanceOf[RemoveActivitiesSummaryView]
    val controller          = new RemoveBusinessTypesSummaryController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      removeServiceHelper,
      router,
      cc = mockMcc,
      view = view
    )
  }

  "A successful result is returned" when {
    "the user visits the page" which {
      "contains the correct content" in new Fixture {
        // scalastyle:off magic.number
        val now = LocalDate.of(2014, 1, 1)
        mockCacheFetch(Some(RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)), Some(DateOfChange(now)))))

        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        validateTitle(s"${messages("title.cya")} - ${messages("summary.updateinformation")}")(implicitly, doc)
        doc.getElementsByTag("h1").text                          must include(messages("title.cya"))
        doc.getElementsByClass("govuk-summary-list__value").text must include(MoneyServiceBusiness.getMessage())
        doc.getElementsByClass("govuk-summary-list__value").text must include(DateHelper.formatDate(now))
      }
    }

    "the user presses the button" which {
      "updates the application with the new data" in new Fixture {

        val flowModel = RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)))

        mockCacheFetch(Some(flowModel), Some(RemoveBusinessTypeFlowModel.key))

        when(removeServiceHelper.removeBusinessMatchingBusinessTypes(any(), eqTo(flowModel))(any()))
          .thenReturn(OptionT.liftF(Future.successful(mock[BusinessMatching])))

        when(removeServiceHelper.removeFitAndProper(any(), eqTo(flowModel))(any()))
          .thenReturn(OptionT.liftF[Future, Seq[ResponsiblePerson]](Future.successful(Seq.empty)))

        when(removeServiceHelper.removeTradingPremisesBusinessTypes(any(), eqTo(flowModel))(any()))
          .thenReturn(OptionT.liftF[Future, Seq[TradingPremises]](Future.successful(Seq.empty)))

        when(removeServiceHelper.removeSectionData(any(), eqTo(flowModel))(any()))
          .thenReturn(OptionT.liftF[Future, Seq[Cache]](Future.successful(Seq.empty)))

        when(removeServiceHelper.removeFlowData(any())(any()))
          .thenReturn(
            OptionT.liftF[Future, RemoveBusinessTypeFlowModel](Future.successful(RemoveBusinessTypeFlowModel()))
          )

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
