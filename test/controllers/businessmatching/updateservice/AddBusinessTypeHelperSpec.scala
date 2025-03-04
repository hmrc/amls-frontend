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

package controllers.businessmatching.updateservice

import cats.implicits._
import config.ApplicationConfig
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessActivitiesGenerator
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities => BABusinessActivities, InvolvedInOtherNo, TaxMatters, WhoIsYourAccountant}
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.flowmanagement.AddBusinessTypeFlowModel
import models.supervision._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify}
import play.api.test.Helpers._
import services.{ResponsiblePeopleService, TradingPremisesService}
import utils._

import java.time.LocalDate

//noinspection ScalaStyle
class AddBusinessTypeHelperSpec
    extends AmlsSpec
    with BusinessActivitiesGenerator
    with ResponsiblePersonGenerator
    with FutureAssertions {

  trait Fixture extends DependencyMocks { self =>

    val tradingPremisesService   = mock[TradingPremisesService]
    val mockUpdateServiceHelper  = mock[AddBusinessTypeHelper]
    val responsiblePeopleService = mock[ResponsiblePeopleService]
    val mockApplicationConfig    = mock[ApplicationConfig]

    val SUT = new AddBusinessTypeHelper()(
      mockCacheConnector,
      tradingPremisesService,
      responsiblePeopleService,
      mockApplicationConfig
    )

    val businessActivitiesSection = BABusinessActivities(
      involvedInOther = Some(InvolvedInOtherNo),
      whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
      accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
      taxMatters = Some(TaxMatters(true)),
      hasAccepted = true
    )
  }

  "updateBusinessActivities" must {
    "remove the accountancy data from the 'business activities' section" in new Fixture {
      mockCacheUpdate[BABusinessActivities](
        Some(models.businessactivities.BusinessActivities.key),
        businessActivitiesSection
      )

      val model = AddBusinessTypeFlowModel(activity = Some(AccountancyServices))
      for {
        result <- SUT.updateBusinessActivities("internalId", model)
      } yield {
        result.involvedInOther mustBe Some(InvolvedInOtherNo)
        result.whoIsYourAccountant          must not be defined
        result.accountantForAMLSRegulations must not be defined
        result.taxMatters                   must not be defined
        result.hasAccepted mustBe true
      }
    }

    "not touch the accountancy data if the activity is not 'accountancy services'" in new Fixture {
      mockCacheUpdate[BABusinessActivities](
        Some(models.businessactivities.BusinessActivities.key),
        businessActivitiesSection
      )

      val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

      for {
        result <- SUT.updateBusinessActivities("internalId", model)
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
          Some(Supervision.key)
        )

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))))),
          Some(BusinessMatching.key)
        )

        mockCacheSave(Supervision(hasAccepted = true), Some(Supervision.key))

        SUT.updateSupervision("internalId").returnsSome(Supervision(hasAccepted = true))

        verify(mockCacheConnector).save(any(), eqTo(Supervision.key), eqTo(Supervision(hasAccepted = true)))(any())
      }
    }

    "leave the supervision section alone" when {
      "the business has ASP" in new Fixture {
        val supervisionModel = Supervision(
          Some(
            AnotherBodyYes(
              "Some supervisor",
              Some(SupervisionStart(LocalDate.now)),
              Some(SupervisionEnd(LocalDate.now)),
              Some(SupervisionEndReasons("no reason"))
            )
          ),
          Some(ProfessionalBodyMemberNo),
          None,
          Some(ProfessionalBodyNo)
        )

        mockCacheFetch[Supervision](Some(supervisionModel), Some(Supervision.key))

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(AccountancyServices))))),
          Some(BusinessMatching.key)
        )

        SUT.updateSupervision("internalId").returnsSome(supervisionModel)

        verify(mockCacheConnector, never).save(any(), any(), any())(any())
      }
    }

    "the business has TCSP" in new Fixture {
      val supervisionModel = Supervision(
        Some(
          AnotherBodyYes(
            "Some supervisor",
            Some(SupervisionStart(LocalDate.now)),
            Some(SupervisionEnd(LocalDate.now)),
            Some(SupervisionEndReasons("no reason"))
          )
        ),
        Some(ProfessionalBodyMemberNo),
        None,
        Some(ProfessionalBodyNo)
      )

      mockCacheFetch[Supervision](Some(supervisionModel), Some(Supervision.key))

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))))),
        Some(BusinessMatching.key)
      )

      SUT.updateSupervision("internalId").returnsSome(supervisionModel)

      verify(mockCacheConnector, never).save(any(), any(), any())(any())
    }
  }

  "updateBusinessMatching" must {
    "update the current activities and msb services" when {
      "there are no msb services and the new activity is not MSB and there are existing activities" in new Fixture {

        val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

        val startResultMatching = BusinessMatching(
          activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))),
          hasAccepted = true,
          hasChanged = true
        )
        val endResultMatching   = BusinessMatching(
          activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, HighValueDealing))),
          hasAccepted = true,
          hasChanged = true
        )

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices))))),
          Some(BusinessMatching.key)
        )

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)
        SUT.updateBusinessMatching("internalId", model).returnsSome(endResultMatching)
      }

      "there are no msb services and the new activity is not MSB and there are no existing activities" in new Fixture {

        val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

        val startResultMatching =
          BusinessMatching(activities = Some(BMBusinessActivities(Set())), hasAccepted = true, hasChanged = true)

        val endResultMatching = BusinessMatching(
          activities = Some(BMBusinessActivities(Set(HighValueDealing))),
          hasAccepted = true,
          hasChanged = true
        )

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set())))),
          Some(BusinessMatching.key)
        )

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)
        SUT.updateBusinessMatching("internalId", model).returnsSome(endResultMatching)
      }

      "there are additional msb services and the activity is MSB and there are existing activities" in new Fixture {

        val model = AddBusinessTypeFlowModel(
          activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal)))
        )

        val startResultMatching = BusinessMatching(
          activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
          hasAccepted = true,
          hasChanged = true,
          msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
        )

        val endResultMatching = BusinessMatching(
          activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
          hasAccepted = true,
          hasChanged = true,
          msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal)))
        )

        mockCacheFetch[BusinessMatching](
          Some(
            BusinessMatching(
              activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
              msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal)))
            )
          ),
          Some(BusinessMatching.key)
        )

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)
        SUT.updateBusinessMatching("internalId", model).returnsSome(endResultMatching)
      }

      "there are msb services and the new activity is MSB and there are no existing activities" in new Fixture {

        val model = AddBusinessTypeFlowModel(
          activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal)))
        )

        val startResultMatching =
          BusinessMatching(activities = Some(BMBusinessActivities(Set())), hasAccepted = true, hasChanged = true)

        val endResultMatching = BusinessMatching(
          activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness))),
          hasAccepted = true,
          hasChanged = true,
          msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal)))
        )

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set())))),
          Some(BusinessMatching.key)
        )

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)
        SUT.updateBusinessMatching("internalId", model).returnsSome(endResultMatching)
      }
    }
  }

  "clearFlowModel" must {
    "set an empty model back into the cache" in new Fixture {
      mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(Some(HighValueDealing)))

      SUT.clearFlowModel("internalId").returnsSome(AddBusinessTypeFlowModel())
    }
  }

  "updateHasAcceptedFlag" must {
    "save the flow model with 'hasAccepted' = true" in new Fixture {
      mockCacheSave[AddBusinessTypeFlowModel]

      await(SUT.updateHasAcceptedFlag("internalId", AddBusinessTypeFlowModel()).value)

      verify(mockCacheConnector).save[AddBusinessTypeFlowModel](
        any(),
        eqTo(AddBusinessTypeFlowModel.key),
        eqTo(AddBusinessTypeFlowModel(hasAccepted = true))
      )(any())
    }
  }

  "updateServicesRegister" must {
    "add the activity to the current activities in the register" when {
      "a ServicesRegister model is already available with pre-existing activities" in new Fixture {
        mockCacheUpdate(Some(ServiceChangeRegister.key), ServiceChangeRegister(Some(Set(MoneyServiceBusiness))))

        SUT
          .updateServicesRegister("internalId", AddBusinessTypeFlowModel(Some(BillPaymentServices)))
          .returnsSome(ServiceChangeRegister(Some(Set(MoneyServiceBusiness, BillPaymentServices))))
      }

      "a ServiceChangeRegister does not exist or has no pre-existing activities" in new Fixture {
        mockCacheUpdate(Some(ServiceChangeRegister.key), ServiceChangeRegister())

        SUT
          .updateServicesRegister("internalId", AddBusinessTypeFlowModel(Some(MoneyServiceBusiness)))
          .returnsSome(ServiceChangeRegister(Some(Set(MoneyServiceBusiness))))
      }
    }
  }
}
