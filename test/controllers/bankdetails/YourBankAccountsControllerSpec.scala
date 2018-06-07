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

package controllers.bankdetails

import models.bankdetails._
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks, StatusConstants}

import scala.collection.JavaConversions._

class YourBankAccountsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val completeModel1 = BankDetails(
      Some(PersonalAccount),
      Some("Completed First Account Name"),
      Some(UKAccount("12341234", "000000"))
    )
    val completeModel2 = BankDetails(
      Some(BelongsToBusiness),
      Some("Completed Second Account Name"),
      Some(UKAccount("12341234", "000000"))
    )

    val completeModel3 = BankDetails(
      Some(BelongsToOtherBusiness),
      Some("Completed Third Account Name"),
      Some(UKAccount("12341234", "000000"))
    )


    val deletedCompleteModel4 = completeModel3.copy(
      status = Some(StatusConstants.Deleted),
      accountName = Some("Completed Deleted Fourth Account Name")
    )

    val inCompleteModel1 = BankDetails(
      Some(PersonalAccount),
      Some("Incomplete First Account Name"),
      Some(UKAccount("12341234", "000000"))
    )
    val inCompleteModel2 = BankDetails(
      Some(BelongsToBusiness),
      Some("Incomplete Second Account Name"),
      Some(UKAccount("12341234", "000000"))
    )

    val inCompleteModel3 = BankDetails(
      Some(BelongsToOtherBusiness),
      Some("Incomplete Third Account Name"),
      Some(UKAccount("12341234", "000000"))
    )


    val deletedInCompleteModel4 = inCompleteModel3.copy(
      status = Some(StatusConstants.Deleted),
      accountName = Some("Incomplete delete Fourth Account Name")
    )

    val controller = new YourBankAccountsController(
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector,
      statusService = mockStatusService
    )
  }

  "Get" must {
    "load the 'your bank accounts' screen with a list of complete and incomplete items" in new Fixture {
//      mockCacheFetch[Seq[BankDetails]](Some(Seq(completeModel1, completeModel2, completeModel3, deletedCompleteModel4,
//        inCompleteModel1, inCompleteModel2, inCompleteModel3, deletedInCompleteModel4)))
//      mockApplicationStatus(SubmissionReady)
//      val result = controller.get()(request)
//      status(result) must be(OK)
//      contentAsString(result) mustNot include(Messages("bankdetails.summary.nobank.account"))
//      contentAsString(result) must include(Messages("bankdetails.summary.incomplete"))
//      contentAsString(result) must include(Messages("bankdetails.summary.complete"))
//      contentAsString(result) must include("Completed First Account Name")
//      contentAsString(result) must include("Completed Second Account Name")
//      contentAsString(result) must include("Completed Third Account Name")
//      contentAsString(result) mustNot include("Completed Deleted Fourth Account Name")
//      contentAsString(result) must include("Incompleted First Account Name")
//      contentAsString(result) must include("Incompleted Second Account Name")
//      contentAsString(result) must include("Incompleted Third Account Name")
//      contentAsString(result) mustNot include("Incomplete Deleted fourth Account Name")
    }


    "load the 'your bank accounts' screen with a list of incomplete items only" in new Fixture {


    }

    "load the 'your bank accounts screen with a list of complete items only" in new Fixture {


    }


    "check that the 'your bank accounts screen does not include any deleted items" in new Fixture {

    }


    "load the 'your bank accounts screen with empty lists" in new Fixture {
            val model = BankDetails(None, None)
            mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
            mockApplicationStatus(SubmissionReady)
            val result = controller.get()(request)
            status(result) must be(OK)
            contentAsString(result) must include(Messages("bankdetails.summary.nobank.account"))
            contentAsString(result) mustNot include(Messages("bankdetails.summary.incomplete"))
            contentAsString(result) mustNot include(Messages("bankdetails.summary.complete"))
    }


    "Can edit if status is not completed" in new Fixture {
      mockApplicationStatus(NotCompleted)


    }

    "Can edit if status is submission ready" in new Fixture {
      mockApplicationStatus(SubmissionReady)


    }

    "cannot edit if status is approved" in new Fixture {
      mockApplicationStatus(SubmissionDecisionApproved)


    }
  }
}

