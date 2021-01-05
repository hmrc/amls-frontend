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

package controllers.businessactivities

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businessactivities._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import utils.AmlsSpec
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.businessactivities.summary
import scala.concurrent.{ExecutionContext, Future}

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self => val request = addToken(authRequest)
    implicit val ec = app.injector.instanceOf[ExecutionContext]

    lazy val view = app.injector.instanceOf[summary]
    val controller = new SummaryController (
      dataCache = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      summary = view
    )

    val mockCacheMap = mock[CacheMap]

    val completeModel = BusinessActivities(
      involvedInOther = Some(BusinessActivitiesValues.DefaultInvolvedInOther),
      expectedBusinessTurnover = Some(BusinessActivitiesValues.DefaultBusinessTurnover),
      expectedAMLSTurnover = Some(BusinessActivitiesValues.DefaultAMLSTurnover),
      businessFranchise = Some(BusinessActivitiesValues.DefaultBusinessFranchise),
      transactionRecord = Some(BusinessActivitiesValues.DefaultTransactionRecord),
      customersOutsideUK = Some(BusinessActivitiesValues.DefaultCustomersOutsideUK),
      ncaRegistered = Some(BusinessActivitiesValues.DefaultNCARegistered),
      accountantForAMLSRegulations = Some(BusinessActivitiesValues.DefaultAccountantForAMLSRegulations),
      riskAssessmentPolicy = Some(BusinessActivitiesValues.DefaultRiskAssessments),
      howManyEmployees = Some(BusinessActivitiesValues.DefaultHowManyEmployees),
      identifySuspiciousActivity = Some(BusinessActivitiesValues.DefaultIdentifySuspiciousActivity),
      whoIsYourAccountant = Some(BusinessActivitiesValues.DefaultWhoIsYourAccountant),
      taxMatters = Some(BusinessActivitiesValues.DefaultTaxMatters),
      transactionRecordTypes = Some(BusinessActivitiesValues.DefaultTransactionRecordTypes),
      hasChanged = false
    )
  }

  "Get" must {

    val bmBusinessActivities = Some(BMBusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)))

    "load the summary page when section data is available" in new Fixture {

      val model = BusinessActivities(None)
      when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetchAll(any())(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
        .thenReturn(Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetchAll(any())(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
        .thenReturn(None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "show edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {
        when(controller.dataCache.fetchAll(any())(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(completeModel))


        when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("involvedinother-edit").html() must include(Messages("button.edit"))
        document.getElementById("expectedbusinessturnover-edit").html() must include(Messages("button.edit"))
        document.getElementById("expectedamlsturnover-edit").html() must include(Messages("button.edit"))
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        when(controller.dataCache.fetchAll(any())(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(completeModel))

        when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("involvedinother-edit").html() must include(Messages("button.edit"))
        document.getElementById("expectedbusinessturnover-edit").html() must include(Messages("button.edit"))
        document.getElementById("expectedamlsturnover-edit").html() must include(Messages("button.edit"))
      }
    }
  }

  "post is called" must {
    "respond with OK and redirect to the registration progress page" when {

      "all questions are complete" in new Fixture {

        val emptyCache = CacheMap("", Map.empty)

        val newRequest = requestWithUrlEncodedBody( "hasAccepted" -> "true")

        when(controller.dataCache.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(completeModel.copy(hasAccepted = false))))

        when(controller.dataCache.save[BusinessActivities](any(), eqTo(BusinessActivities.key), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

    }
  }
}

object BusinessActivitiesValues {
  val DefaultFranchiseName = "DEFAULT FRANCHISE NAME"
  val DefaultSoftwareName = "DEFAULT SOFTWARE"
  val DefaultBusinessTurnover = ExpectedBusinessTurnover.First
  val DefaultAMLSTurnover = ExpectedAMLSTurnover.First
  val DefaultInvolvedInOtherDetails = "DEFAULT INVOLVED"
  val DefaultInvolvedInOther = InvolvedInOtherYes(DefaultInvolvedInOtherDetails)
  val DefaultBusinessFranchise = BusinessFranchiseYes(DefaultFranchiseName)
  val DefaultTransactionRecord = true
  val DefaultTransactionRecordTypes = TransactionTypes(Set(Paper, DigitalSoftware(DefaultSoftwareName)))
  val DefaultCustomersOutsideUK = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))
  val DefaultNCARegistered = NCARegistered(true)
  val DefaultAccountantForAMLSRegulations = AccountantForAMLSRegulations(true)
  val DefaultRiskAssessments = RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased)))
  val DefaultHowManyEmployees = HowManyEmployees(Some("5"),Some("4"))
  val DefaultWhoIsYourAccountant = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName("Accountant's name", Some("Accountant's trading name"))),
    Some(WhoIsYourAccountantIsUk(true)),
    Some(UkAccountantsAddress("address1", "address2", Some("address3"), Some("address4"), "POSTCODE"))
  )
  val DefaultIdentifySuspiciousActivity = IdentifySuspiciousActivity(true)
  val DefaultTaxMatters = TaxMatters(false)
}
