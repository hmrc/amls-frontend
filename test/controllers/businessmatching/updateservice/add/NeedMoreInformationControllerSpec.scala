/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.businessmatching.updateservice.add

import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.{AddBusinessTypeFlowModel, NeedMoreInformationPageId}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils._

class NeedMoreInformationControllerSpec extends AmlsSpec with MockitoSugar with FutureAssertions with ScalaFutures {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[AddBusinessTypeHelper]

    val controller = new NeedMoreInformationController(
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[AddBusinessTypeFlowModel],
      cc = mockMcc
    )

    val flowModel = AddBusinessTypeFlowModel(Some(AccountancyServices), Some(true))
    mockCacheFetch[AddBusinessTypeFlowModel](Some(flowModel), Some(AddBusinessTypeFlowModel.key))
  }

  "NeedMoreInformationControllerSpec  (Add)" when {

    "get is called" must {
      "return OK with new_service_information view" in new Fixture {

        mockCacheFetch(Some(ServiceChangeRegister(Some(Set(HighValueDealing)))), Some(ServiceChangeRegister.key))

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.newserviceinformation.title"))
      }
    }
  }

  "post is called" must {
    "return OK with registration progress view" in new Fixture {
      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      controller.router.verify("internalId", NeedMoreInformationPageId, new AddBusinessTypeFlowModel())
    }
  }
}
