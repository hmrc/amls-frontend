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

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.BusinessActivity.AccountancyServices
import models.flowmanagement.{NeedToUpdatePageId, RemoveBusinessTypeFlowModel}
import org.jsoup.Jsoup
import play.api.test.Helpers.{OK, contentAsString, status, _}
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.remove.NeedMoreInformationView

class NeedMoreInformationControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {
    self =>

    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[NeedMoreInformationView]
    val controller = new NeedMoreInformationController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[RemoveBusinessTypeFlowModel],
      cc = mockMcc,
      view = view
    )
  }

  "NeedToUpdateController (Remove)" when {

    "get is called" must {

      "return OK with NeedMoreInformationView" in new Fixture {
        mockCacheFetch(
          Some(RemoveBusinessTypeFlowModel(Some(Set(AccountancyServices)))),
          Some(RemoveBusinessTypeFlowModel.key)
        )

        val result = controller.get()(request)
        status(result)                               must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(
          messages("businessmatching.updateservice.updateotherinformation.title")
        )
      }
    }

    "post is called" must {
      "redirect to next page" in new Fixture {
        mockCacheFetch(Some(RemoveBusinessTypeFlowModel()), Some(RemoveBusinessTypeFlowModel.key))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        controller.router.verify("internalId", NeedToUpdatePageId, new RemoveBusinessTypeFlowModel())
      }
    }
  }
}
