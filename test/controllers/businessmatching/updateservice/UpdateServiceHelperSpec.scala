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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessActivitiesGenerator
import models.businessactivities._
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, ServiceChangeRegister}
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.businessmatching.{BusinessMatchingMsbServices => BMMsbServices}
import models.flowmanagement.AddServiceFlowModel
import models.responsiblepeople.ResponsiblePeople
import models.supervision._
import org.joda.time.LocalDate
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Gen
import org.scalatest.MustMatchers
import play.api.test.Helpers._
import services.{ResponsiblePeopleService, TradingPremisesService}
import utils.{AuthorisedFixture, DependencyMocks, FutureAssertions, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits.global

//noinspection ScalaStyle
class UpdateServiceHelperSpec extends AmlsSpec
  with BusinessActivitiesGenerator
  with ResponsiblePersonGenerator
  with FutureAssertions {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val tradingPremisesService = mock[TradingPremisesService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]
    val responsiblePeopleService = mock[ResponsiblePeopleService]

    val helper = new UpdateServiceHelper(
      self.authConnector,
      mockCacheConnector,
      tradingPremisesService,
      responsiblePeopleService
    )

    val businessActivitiesSection = BusinessActivities(
      involvedInOther = Some(InvolvedInOtherNo),
      whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
      accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
      taxMatters = Some(TaxMatters(true)),
      hasAccepted = true
    )
  }

  "updateBusinessActivities" must {
    "remove the accountancy data from the 'business activities' section" in new Fixture {
      mockCacheUpdate[BusinessActivities](Some(BusinessActivities.key), businessActivitiesSection)

      val model = AddServiceFlowModel(activity = Some(AccountancyServices))
      for {
        result <- helper.updateBusinessActivities(model)
      } yield {
        result.involvedInOther mustBe Some(InvolvedInOtherNo)
        result.whoIsYourAccountant must not be defined
        result.accountantForAMLSRegulations must not be defined
        result.taxMatters must not be defined
        result.hasAccepted mustBe true
      }
    }

    "not touch the accountancy data if the activity is not 'accountancy services'" in new Fixture {
      mockCacheUpdate[BusinessActivities](Some(BusinessActivities.key), businessActivitiesSection)

      val model = AddServiceFlowModel(activity = Some(HighValueDealing))

      for {
        result <- helper.updateBusinessActivities(model)
      } yield {
        result.whoIsYourAccountant mustBe defined
        result.accountantForAMLSRegulations mustBe Some(AccountantForAMLSRegulations(true))
        result.taxMatters mustBe Some(TaxMatters(true))
        result.hasAccepted mustBe true
      }
    }
  }

  "updateSupervision" must {
    "return a blank supervision element" when {
      "the business doesn't have ASP or TSCP" in new Fixture {
        mockCacheFetch[Supervision](
          Some(Supervision(Some(AnotherBodyNo), Some(ProfessionalBodyMemberNo), None, Some(ProfessionalBodyNo))),
          Some(Supervision.key))

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))))),
          Some(BusinessMatching.key))

        mockCacheSave(Supervision(hasAccepted = true), Some(Supervision.key))

        helper.updateSupervision.returnsSome(Supervision(hasAccepted = true))

        verify(mockCacheConnector).save(eqTo(Supervision.key), eqTo(Supervision(hasAccepted = true)))(any(), any(), any())
      }
    }

    "leave the supervision section alone" when {
      "the business has ASP" in new Fixture {
        val supervisionModel = Supervision(Some(AnotherBodyYes("Some supervisor", LocalDate.now, LocalDate.now, "no reason")),
          Some(ProfessionalBodyMemberNo),
          None,
          Some(ProfessionalBodyNo))

        mockCacheFetch[Supervision](Some(supervisionModel), Some(Supervision.key))

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(AccountancyServices))))),
          Some(BusinessMatching.key))

        helper.updateSupervision.returnsSome(supervisionModel)

        verify(mockCacheConnector, never).save(any(), any())(any(), any(), any())
      }
    }

    "the business has TCSP" in new Fixture {
      val supervisionModel = Supervision(Some(AnotherBodyYes("Some supervisor", LocalDate.now, LocalDate.now, "no reason")),
        Some(ProfessionalBodyMemberNo),
        None,
        Some(ProfessionalBodyNo))

      mockCacheFetch[Supervision](Some(supervisionModel), Some(Supervision.key))

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))))),
        Some(BusinessMatching.key))

      helper.updateSupervision.returnsSome(supervisionModel)

      verify(mockCacheConnector, never).save(any(), any())(any(), any(), any())
    }
  }

  "updateBusinessMatching" must {
    "update the current activities and msb services" when {
      "there are no msb services and the new activity is not MSB and there are existing activities" in new Fixture {

        val model = AddServiceFlowModel(activity = Some(HighValueDealing))

        var endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, HighValueDealing))), hasAccepted = true, hasChanged = true)

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))))),
          Some(BusinessMatching.key))

        mockCacheUpdate(Some(BusinessMatching.key), endResultMatching )
        helper.updateBusinessMatching(model).returnsSome(endResultMatching)
      }

      "there are no msb services and the new activity is not MSB and there are no existing activities" in new Fixture {

        val model = AddServiceFlowModel(activity = Some(HighValueDealing))

        var endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, HighValueDealing))), hasAccepted = true, hasChanged = true)
        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))))),
          Some(BusinessMatching.key))
        mockCacheUpdate(Some(BusinessMatching.key), endResultMatching )
        helper.updateBusinessMatching(model).returnsSome(endResultMatching)
      }

      "there are msb services and the new activity is MSB and there are existing activities" in new Fixture {

        val model = AddServiceFlowModel(
          activity = Some(MoneyServiceBusiness),
          msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
        )
        var endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
                                hasAccepted = true,
                                hasChanged = true,
                                msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal))))
        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))))),
          Some(BusinessMatching.key))
        mockCacheUpdate(Some(BusinessMatching.key),  endResultMatching )
        helper.updateBusinessMatching(model).returnsSome(endResultMatching)
      }

      "there are msb services and the new activity is MSB and there are no existing activities" in new Fixture {

        val model = AddServiceFlowModel(
          activity = Some(MoneyServiceBusiness),
          msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
        )
        var endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness))),
                                hasAccepted = true,
                                hasChanged = true,
                                msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal))))
        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = None)),
          Some(BusinessMatching.key))
        mockCacheUpdate(Some(BusinessMatching.key),  endResultMatching )
        helper.updateBusinessMatching(model).returnsSome(endResultMatching)
      }
    }
  }


  "updateResponsiblePeople" must {
    "set the fit and proper flag on the right people according to the indices" when {
      "adding the TCSP business type" in new Fixture {
        val people = Gen.listOfN(5, responsiblePersonGen).sample.get map {
          _.copy(hasAlreadyPassedFitAndProper = Some(false))
        }

        val updatedPeople = people map { _.copy(hasAlreadyPassedFitAndProper = Some(true)) }

        mockCacheUpdate(Some(ResponsiblePeople.key), people)

        val model = AddServiceFlowModel(
          Some(TrustAndCompanyServices),
          fitAndProper = Some(true),
          responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(0, 1, 2, 4, 5)))
        )

        when {
          responsiblePeopleService.updateFitAndProperFlag(any(), any())
        } thenReturn updatedPeople

        helper.updateResponsiblePeople(model).returnsSome(updatedPeople)
      }
    }

    "not touch the responsible people" when {
      "adding a business type that isn't TCSP" in new Fixture {
        val people = Gen.listOfN(5, responsiblePersonGen).sample.get map {
          _.copy(hasAlreadyPassedFitAndProper = Some(false))
        }

        mockCacheUpdate(Some(ResponsiblePeople.key), people)

        val model = AddServiceFlowModel(Some(HighValueDealing))

        helper.updateResponsiblePeople(model).returnsSome(people)

        verify(responsiblePeopleService, never).updateFitAndProperFlag(any(), any())
      }
    }
  }

  "clearFlowModel" must {
    "set an empty model back into the cache" in new Fixture {
      mockCacheUpdate(Some(AddServiceFlowModel.key), AddServiceFlowModel(Some(HighValueDealing), fitAndProper = Some(true)))

      helper.clearFlowModel().returnsSome(AddServiceFlowModel())
    }
  }

  "updateHasAcceptedFlag" must {
    "save the flow model with 'hasAccepted' = true" in new Fixture {
      mockCacheSave[AddServiceFlowModel]

      await(helper.updateHasAcceptedFlag(AddServiceFlowModel()).value)

      verify(mockCacheConnector).save[AddServiceFlowModel](eqTo(AddServiceFlowModel.key), eqTo(AddServiceFlowModel(hasAccepted = true)))(any(), any(), any())
    }
  }

  "updateServicesRegister" must {
    "add the activity to the current activities in the register" when {
      "a ServicesRegister model is already available with pre-existing activities" in new Fixture {
        mockCacheUpdate(Some(ServiceChangeRegister.key), ServiceChangeRegister(Some(Set(MoneyServiceBusiness))))

        helper.updateServicesRegister(AddServiceFlowModel(Some(BillPaymentServices)))
          .returnsSome(ServiceChangeRegister(Some(Set(MoneyServiceBusiness, BillPaymentServices))))
      }

      "a ServiceChangeRegister does not exist or has no pre-existing activities" in new Fixture {
        mockCacheUpdate(Some(ServiceChangeRegister.key), ServiceChangeRegister())

        helper.updateServicesRegister(AddServiceFlowModel(Some(MoneyServiceBusiness)))
          .returnsSome(ServiceChangeRegister(Some(Set(MoneyServiceBusiness))))
      }
    }
  }
}
