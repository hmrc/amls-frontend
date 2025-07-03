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

package models.businessactivities

import models.businessactivities.TransactionTypes.{DigitalSoftware, Paper}
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => ba, _}
import models.registrationprogress._
import models.{Country, DateOfChange}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import services.cache.Cache
import utils.AmlsSpec

import java.time.LocalDate

class BusinessActivitiesSpec extends AmlsSpec {

  val DefaultFranchiseName                                              = "DEFAULT FRANCHISE NAME"
  val DefaultSoftwareName                                               = "DEFAULT SOFTWARE"
  val DefaultBusinessTurnover: ExpectedBusinessTurnover                 = ExpectedBusinessTurnover.First
  val DefaultAMLSTurnover: ExpectedAMLSTurnover                         = ExpectedAMLSTurnover.First
  val DefaultInvolvedInOtherDetails                                     = "DEFAULT INVOLVED"
  val DefaultInvolvedInOther: InvolvedInOtherYes                        = InvolvedInOtherYes(DefaultInvolvedInOtherDetails)
  val DefaultBusinessFranchise: BusinessFranchiseYes                    = BusinessFranchiseYes(DefaultFranchiseName)
  val DefaultTransactionRecord                                          = true
  val DefaultTransactionRecordTypes: TransactionTypes                   = TransactionTypes(
    Set(Paper, DigitalSoftware(DefaultSoftwareName))
  )
  val DefaultCustomersOutsideUK: CustomersOutsideUK                     = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))
  val DefaultNCARegistered: NCARegistered                               = NCARegistered(true)
  val DefaultAccountantForAMLSRegulations: AccountantForAMLSRegulations = AccountantForAMLSRegulations(true)
  val DefaultRiskAssessments: RiskAssessmentPolicy                      =
    RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased)))
  val DefaultHowManyEmployees: HowManyEmployees                         = HowManyEmployees(Some("5"), Some("4"))
  val DefaultWhoIsYourAccountant: WhoIsYourAccountant                   = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName("Accountant's name", Some("Accountant's trading name"))),
    Some(WhoIsYourAccountantIsUk(true)),
    Some(UkAccountantsAddress("address1", Some("address2"), Some("address3"), Some("address4"), "POSTCODE"))
  )
  val DefaultIdentifySuspiciousActivity: IdentifySuspiciousActivity     = IdentifySuspiciousActivity(true)
  val DefaultTaxMatters: TaxMatters                                     = TaxMatters(false)

  val NewFranchiseName                                              = "NEW FRANCHISE NAME"
  val NewBusinessFranchise: BusinessFranchiseYes                    = BusinessFranchiseYes(NewFranchiseName)
  val NewInvolvedInOtherDetails: String                             = "NEW INVOLVED"
  val NewInvolvedInOther: InvolvedInOtherYes                        = InvolvedInOtherYes(NewInvolvedInOtherDetails)
  val NewBusinessTurnover: ExpectedBusinessTurnover                 = ExpectedBusinessTurnover.Second
  val NewAMLSTurnover: ExpectedAMLSTurnover                         = ExpectedAMLSTurnover.Second
  val NewTransactionRecord                                          = false
  val NewCustomersOutsideUK: CustomersOutsideUK                     = CustomersOutsideUK(None)
  val NewNCARegistered: NCARegistered                               = NCARegistered(false)
  val NewAccountantForAMLSRegulations: AccountantForAMLSRegulations = AccountantForAMLSRegulations(false)
  val NewRiskAssessment: RiskAssessmentPolicy                       =
    RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set()))
  val NewHowManyEmployees: HowManyEmployees                         = HowManyEmployees(Some("2"), Some("3"))
  val NewIdentifySuspiciousActivity: IdentifySuspiciousActivity     = IdentifySuspiciousActivity(true)
  val NewWhoIsYourAccountant: WhoIsYourAccountant                   = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName("newName", Some("newTradingName"))),
    Some(WhoIsYourAccountantIsUk(true)),
    Some(UkAccountantsAddress("98E", Some("Building1"), Some("street1"), Some("road1"), "AA11 1AA"))
  )
  val NewTaxMatters: TaxMatters                                     = TaxMatters(true)

  val bmBusinessActivitiesWithoutASP: ba = ba(Set(EstateAgentBusinessService))

  val completeModel: BusinessActivities = BusinessActivities(
    involvedInOther = Some(DefaultInvolvedInOther),
    expectedBusinessTurnover = Some(DefaultBusinessTurnover),
    expectedAMLSTurnover = Some(DefaultAMLSTurnover),
    businessFranchise = Some(DefaultBusinessFranchise),
    transactionRecord = Some(DefaultTransactionRecord),
    customersOutsideUK = Some(DefaultCustomersOutsideUK),
    ncaRegistered = Some(DefaultNCARegistered),
    accountantForAMLSRegulations = Some(DefaultAccountantForAMLSRegulations),
    riskAssessmentPolicy = Some(DefaultRiskAssessments),
    howManyEmployees = Some(DefaultHowManyEmployees),
    identifySuspiciousActivity = Some(DefaultIdentifySuspiciousActivity),
    whoIsYourAccountant = Some(DefaultWhoIsYourAccountant),
    taxMatters = Some(DefaultTaxMatters),
    transactionRecordTypes = Some(DefaultTransactionRecordTypes),
    hasChanged = false,
    hasAccepted = true
  )

  val completeModelOtherYesNoExpectedTurnover: BusinessActivities = completeModel.copy(
    expectedBusinessTurnover = None,
    hasChanged = false,
    hasAccepted = true
  )

  val completeModelWithoutCustUK: BusinessActivities = BusinessActivities(
    involvedInOther = Some(DefaultInvolvedInOther),
    expectedBusinessTurnover = Some(DefaultBusinessTurnover),
    expectedAMLSTurnover = Some(DefaultAMLSTurnover),
    businessFranchise = Some(DefaultBusinessFranchise),
    transactionRecord = Some(DefaultTransactionRecord),
    customersOutsideUK = None,
    ncaRegistered = Some(DefaultNCARegistered),
    accountantForAMLSRegulations = Some(DefaultAccountantForAMLSRegulations),
    riskAssessmentPolicy = Some(DefaultRiskAssessments),
    howManyEmployees = Some(DefaultHowManyEmployees),
    identifySuspiciousActivity = Some(DefaultIdentifySuspiciousActivity),
    whoIsYourAccountant = Some(DefaultWhoIsYourAccountant),
    taxMatters = Some(DefaultTaxMatters),
    transactionRecordTypes = Some(DefaultTransactionRecordTypes),
    hasChanged = false,
    hasAccepted = true
  )

  val completeModelWithoutAccountantAdvice: BusinessActivities = BusinessActivities(
    involvedInOther = Some(DefaultInvolvedInOther),
    expectedBusinessTurnover = Some(DefaultBusinessTurnover),
    expectedAMLSTurnover = Some(DefaultAMLSTurnover),
    businessFranchise = Some(DefaultBusinessFranchise),
    transactionRecord = Some(DefaultTransactionRecord),
    customersOutsideUK = None,
    ncaRegistered = Some(DefaultNCARegistered),
    accountantForAMLSRegulations = None,
    riskAssessmentPolicy = Some(DefaultRiskAssessments),
    howManyEmployees = Some(DefaultHowManyEmployees),
    identifySuspiciousActivity = Some(DefaultIdentifySuspiciousActivity),
    whoIsYourAccountant = None,
    taxMatters = None,
    transactionRecordTypes = Some(DefaultTransactionRecordTypes),
    hasChanged = false,
    hasAccepted = true
  )

  val completeJson: JsObject = Json.obj(
    "involvedInOther"              -> true,
    "details"                      -> DefaultInvolvedInOtherDetails,
    "expectedBusinessTurnover"     -> "01",
    "expectedAMLSTurnover"         -> "01",
    "businessFranchise"            -> true,
    "franchiseName"                -> DefaultFranchiseName,
    "isRecorded"                   -> true,
    "transactionTypes"             -> Json.obj(
      "types"    -> Seq("01", "03"),
      "software" -> DefaultSoftwareName
    ),
    "isOutside"                    -> true,
    "countries"                    -> Json.arr("GB"),
    "ncaRegistered"                -> true,
    "accountantForAMLSRegulations" -> true,
    "hasWrittenGuidance"           -> true,
    "hasPolicy"                    -> true,
    "riskassessments"              -> Seq("01"),
    "employeeCount"                -> "5",
    "employeeCountAMLSSupervision" -> "4",
    "isUK"                         -> true,
    "accountantsName"              -> "Accountant's name",
    "accountantsTradingName"       -> "Accountant's trading name",
    "accountantsAddressLine1"      -> "address1",
    "accountantsAddressLine2"      -> "address2",
    "accountantsAddressLine3"      -> "address3",
    "accountantsAddressLine4"      -> "address4",
    "accountantsAddressPostCode"   -> "POSTCODE",
    "manageYourTaxAffairs"         -> false,
    "hasWrittenGuidance"           -> true,
    "hasChanged"                   -> false,
    "hasAccepted"                  -> true
  )

  val oldFormatJson: JsObject = Json.obj(
    "involvedInOther"              -> true,
    "details"                      -> DefaultInvolvedInOtherDetails,
    "expectedBusinessTurnover"     -> "01",
    "expectedAMLSTurnover"         -> "01",
    "businessFranchise"            -> true,
    "franchiseName"                -> DefaultFranchiseName,
    "isRecorded"                   -> true,
    "transactions"                 -> Seq("01", "03"),
    "digitalSoftwareName"          -> DefaultSoftwareName,
    "isOutside"                    -> true,
    "countries"                    -> Json.arr("GB"),
    "ncaRegistered"                -> true,
    "accountantForAMLSRegulations" -> true,
    "hasWrittenGuidance"           -> true,
    "hasPolicy"                    -> true,
    "riskassessments"              -> Seq("01"),
    "employeeCount"                -> "5",
    "employeeCountAMLSSupervision" -> "4",
    "accountantsName"              -> "Accountant's name",
    "accountantsTradingName"       -> "Accountant's trading name",
    "accountantsAddressLine1"      -> "address1",
    "accountantsAddressLine2"      -> "address2",
    "accountantsAddressLine3"      -> "address3",
    "accountantsAddressLine4"      -> "address4",
    "accountantsAddressPostCode"   -> "POSTCODE",
    "manageYourTaxAffairs"         -> false,
    "hasWrittenGuidance"           -> true,
    "hasChanged"                   -> false,
    "hasAccepted"                  -> true
  )

  val partialModel: BusinessActivities = BusinessActivities(businessFranchise = Some(DefaultBusinessFranchise))

  "BusinessActivities Serialisation" must {
    "serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "deserialise as expected" in {
      completeJson.as[BusinessActivities] must
        be(completeModel)
    }
  }

  it when {
    "hasChanged is missing from the Json" must {
      "Deserialise correctly" in {
        (completeJson - "hasChanged").as[BusinessActivities] must
          be(completeModel)
      }
    }
  }

  "isComplete" must {
    "return false when the model is incomplete" in {
      partialModel.isComplete(None) must be(false)
    }

    "return true when the model is complete" in {
      completeModel.isComplete(None) must be(true)
    }

    "return true when the CustomersOutsideUK is none" in {
      completeModelWithoutCustUK.isComplete(None) must be(true)
    }

    "return false when the regulation questions have no answers and BM activities does not include ASP" in {
      completeModelWithoutAccountantAdvice.isComplete(Some(bmBusinessActivitiesWithoutASP)) must be(false)
    }

    "return true when the regulation questions have no answers and BM activities does include ASP" in {
      completeModelWithoutAccountantAdvice.isComplete(Some(ba(Set(AccountancyServices)))) must be(true)
    }

    "return false if only partial regulation questions have been answered" in {
      val model = completeModelWithoutAccountantAdvice.copy(
        accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
        whoIsYourAccountant = None,
        taxMatters = None
      )

      model.isComplete(Some(ba(Set(HighValueDealing)))) must be(false)
    }

    "return true if partial regulation questions were answered, but accountantForAMLSRegulations is false" in {
      val model = completeModelWithoutAccountantAdvice.copy(
        accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(false)),
        whoIsYourAccountant = None,
        taxMatters = None
      )

      model.isComplete(Some(ba(Set(HighValueDealing)))) must be(true)
    }

    "return false if other business activities true but no expectedBusinessTurnover present" in {
      completeModelOtherYesNoExpectedTurnover.isComplete(None) must be(false)
    }
  }

  "Partially complete BusinessActivities" must {

    val partialJson = Json.obj(
      "businessFranchise" -> true,
      "franchiseName"     -> DefaultFranchiseName,
      "hasChanged"        -> false,
      "hasAccepted"       -> false
    )

    "serialise as expected" in {
      Json.toJson(partialModel) mustBe partialJson
    }

  }

  "Old format BusinessActivities json" must {
    "deserialise correctly" in {
      oldFormatJson.as[BusinessActivities] mustBe completeModel
    }
  }

  "BusinessActivities with all values set as None" must {

    val initial: Option[BusinessActivities] = None

    "return BusinessActivities with InvolvedInOther set and indicate that changes have been made" in {
      val result = initial.involvedInOther(NewInvolvedInOther)
      result must be(BusinessActivities(Some(NewInvolvedInOther), hasChanged = true))
    }

    "return BusinessActivities with ExcpectedBusinessTurnover set and indicate that changes have been made" in {
      val result = initial.expectedBusinessTurnover(NewBusinessTurnover)
      result must be(BusinessActivities(None, Some(NewBusinessTurnover), hasChanged = true))
    }

    "return BusinessActivities with ExpectedAMLSTurnover set and indicate that changes have been made" in {
      val result = initial.expectedAMLSTurnover(NewAMLSTurnover)
      result must be(BusinessActivities(None, None, Some(NewAMLSTurnover), hasChanged = true))
    }

    "return BusinessActivities with BusinessFranchise set and indicate that changes have been made" in {
      val result = initial.businessFranchise(NewBusinessFranchise)
      result must be(BusinessActivities(None, None, None, Some(NewBusinessFranchise), hasChanged = true))
    }

    "return BusinessActivities with TransactionRecord set and indicate that changes have been made" in {
      val result = initial.transactionRecord(NewTransactionRecord)
      result must be(BusinessActivities(None, None, None, None, Some(NewTransactionRecord), hasChanged = true))
    }

    "return BusinessActivities with CustomersOutsideUK set and indicate that changes have been made" in {
      val result = initial.customersOutsideUK(NewCustomersOutsideUK)
      result must be(BusinessActivities(None, None, None, None, None, Some(NewCustomersOutsideUK), hasChanged = true))
    }

    "return BusinessActivities with ncaRegistered set and indicate that changes have been made" in {
      val result = initial.ncaRegistered(NewNCARegistered)
      result must be(BusinessActivities(None, None, None, None, None, None, Some(NewNCARegistered), hasChanged = true))
    }

    "return BusinessActivities with accountantForAMLSRegulations set and indicate that changes have been made" in {
      val result = initial.accountantForAMLSRegulations(Some(NewAccountantForAMLSRegulations))
      result must be(
        BusinessActivities(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some(NewAccountantForAMLSRegulations),
          hasChanged = true
        )
      )
    }

    "return BusinessActivities with RiskAssesment set and indicate that changes have been made" in {
      val result = initial.riskAssessmentPolicy(NewRiskAssessment)
      result must be(BusinessActivities(riskAssessmentPolicy = Some(NewRiskAssessment), hasChanged = true))
    }

    "return BusinessActivities with IdentifySuspiciousActivity set and indicate that changes have been made" in {
      val result = initial.identifySuspiciousActivity(NewIdentifySuspiciousActivity)
      result must be(
        BusinessActivities(identifySuspiciousActivity = Some(NewIdentifySuspiciousActivity), hasChanged = true)
      )
    }

    "return BusinessActivities with WhoIsYourAccountant set and indicate that changes have been made" in {
      val result = initial.whoIsYourAccountant(Some(NewWhoIsYourAccountant))
      result must be(BusinessActivities(whoIsYourAccountant = Some(NewWhoIsYourAccountant), hasChanged = true))
    }

    "return BusinessActivities with TaxMatters set and indicate that changes have been made" in {
      val result = initial.taxMatters(Some(NewTaxMatters))
      result must be(BusinessActivities(taxMatters = Some(NewTaxMatters), hasChanged = true))
    }
  }

  "BusinessActivities class" when {
    "involvedInOther value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.involvedInOther(DefaultInvolvedInOther)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.involvedInOther(InvolvedInOtherNo)
          res.hasChanged      must be(true)
          res.involvedInOther must be(Some(InvolvedInOtherNo))
        }
      }
    }

    "expectedBusinessTurnover value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.expectedBusinessTurnover(DefaultBusinessTurnover)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.expectedBusinessTurnover(NewBusinessTurnover)
          res.hasChanged               must be(true)
          res.expectedBusinessTurnover must be(Some(NewBusinessTurnover))
        }
      }
    }

    "expectedAMLSTurnover value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.expectedAMLSTurnover(DefaultAMLSTurnover)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.expectedAMLSTurnover(NewAMLSTurnover)
          res.hasChanged           must be(true)
          res.expectedAMLSTurnover must be(Some(NewAMLSTurnover))
        }
      }
    }

    "businessFranchise value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.businessFranchise(DefaultBusinessFranchise)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.businessFranchise(NewBusinessFranchise)
          res.hasChanged        must be(true)
          res.businessFranchise must be(Some(NewBusinessFranchise))
        }
      }
    }

    "transactionRecord value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.transactionRecord(DefaultTransactionRecord)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.transactionRecord(NewTransactionRecord)
          res.hasChanged        must be(true)
          res.transactionRecord must be(Some(NewTransactionRecord))
        }
      }
    }

    "customersOutsideUK value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.customersOutsideUK(DefaultCustomersOutsideUK)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.customersOutsideUK(NewCustomersOutsideUK)
          res.hasChanged         must be(true)
          res.customersOutsideUK must be(Some(NewCustomersOutsideUK))
        }
      }
    }

    "ncaRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.ncaRegistered(DefaultNCARegistered)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.ncaRegistered(NewNCARegistered)
          res.hasChanged    must be(true)
          res.ncaRegistered must be(Some(NewNCARegistered))
        }
      }
    }

    "accountantForAMLSRegulations value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.accountantForAMLSRegulations(Some(DefaultAccountantForAMLSRegulations))
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.accountantForAMLSRegulations(Some(NewAccountantForAMLSRegulations))
          res.hasChanged                   must be(true)
          res.accountantForAMLSRegulations must be(Some(NewAccountantForAMLSRegulations))
        }
      }
    }

    "riskAssessmentPolicy value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.riskAssessmentPolicy(DefaultRiskAssessments)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.riskAssessmentPolicy(NewRiskAssessment)
          res.hasChanged           must be(true)
          res.riskAssessmentPolicy must be(Some(NewRiskAssessment))
        }
      }
    }

    "whoIsYourAccountant value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.whoIsYourAccountant(Some(DefaultWhoIsYourAccountant))
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.whoIsYourAccountant(Some(NewWhoIsYourAccountant))
          res.hasChanged          must be(true)
          res.whoIsYourAccountant must be(Some(NewWhoIsYourAccountant))
        }
      }
    }
    "taxMatters value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.taxMatters(Some(DefaultTaxMatters))
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegistered Properties" in {
          val res = completeModel.taxMatters(Some(NewTaxMatters))
          res.hasChanged must be(true)
          res.taxMatters must be(Some(NewTaxMatters))
        }
      }
    }
  }

  "taskRow is called" must {

    val mockCacheMap = mock[Cache]

    "return a not started task row" when {

      "tasks have not been started" in {

        when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(None)

        BusinessActivities.taskRow(mockCacheMap, messages) mustBe TaskRow(
          "businessactivities",
          controllers.businessactivities.routes.WhatYouNeedController.get.url,
          hasChanged = false,
          NotStarted,
          TaskRow.notStartedTag
        )
      }
    }

    "return an incomplete task row" when {

      "activities have not been completed" in {

        when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching()))

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(BusinessActivities()))

        BusinessActivities.taskRow(mockCacheMap, messages) mustBe TaskRow(
          "businessactivities",
          controllers.businessactivities.routes.WhatYouNeedController.get.url,
          hasChanged = false,
          Started,
          TaskRow.incompleteTag
        )
      }
    }

    "return a completed task row" when {

      "mandatory sections have been completed" in {

        val bmBa = ba(
          Set(AccountancyServices),
          None,
          None,
          Some(DateOfChange(LocalDate.now()))
        )

        when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching(
                activities = Some(bmBa)
              )
            )
          )

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(completeModel))
        val respUrl = controllers.businessactivities.routes.SummaryController.get.url
        BusinessActivities.taskRow(mockCacheMap, messages) mustBe TaskRow(
          "businessactivities",
          controllers.routes.YourResponsibilitiesUpdateController.get(respUrl).url,
          hasChanged = false,
          Completed,
          TaskRow.completedTag
        )
      }
    }

    "return an updated task row" when {

      "mandatory sections have been completed and model has changed" in {

        val bmBa = ba(
          Set(AccountancyServices),
          None,
          None,
          Some(DateOfChange(LocalDate.now()))
        )

        when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching(
                activities = Some(bmBa)
              )
            )
          )

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(completeModel.copy(hasChanged = true)))

        BusinessActivities.taskRow(mockCacheMap, messages) mustBe TaskRow(
          "businessactivities",
          controllers.businessactivities.routes.SummaryController.get.url,
          hasChanged = true,
          Updated,
          TaskRow.updatedTag
        )
      }
    }
  }
}
