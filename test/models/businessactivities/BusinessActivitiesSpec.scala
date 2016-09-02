package models.businessactivities

import models.Country
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}

class BusinessActivitiesSpec extends PlaySpec with MockitoSugar {

  val DefaultFranchiseName     = "DEFAULT FRANCHISE NAME"
  val DefaultSoftwareName      = "DEFAULT SOFTWARE"
  val DefaultBusinessTurnover  = ExpectedBusinessTurnover.First
  val DefaultAMLSTurnover      = ExpectedAMLSTurnover.First
  val DefaultInvolvedInOtherDetails = "DEFAULT INVOLVED"
  val DefaultInvolvedInOther   = InvolvedInOtherYes(DefaultInvolvedInOtherDetails)
  val DefaultBusinessFranchise = BusinessFranchiseYes(DefaultFranchiseName)
  val DefaultTransactionRecord = TransactionRecordYes(Set(Paper, DigitalSoftware(DefaultSoftwareName)))
  val DefaultCustomersOutsideUK = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))
  val DefaultNCARegistered      = NCARegistered(true)
  val DefaultAccountantForAMLSRegulations = AccountantForAMLSRegulations(true)
  val DefaultRiskAssessments    = RiskAssessmentPolicyYes(Set(PaperBased))
  val DefaultWhoIsYourAccountant = WhoIsYourAccountant("Accountant's name", Some("Accountant's trading name"),
                                                          UkAccountantsAddress("address1", "address2", Some("address3"), Some("address4"), "POSTCODE" ),
                                                        AccountantDoesAlsoDealWithTax("11Character"))
  val DefaultIdentifySuspiciousActivity  = IdentifySuspiciousActivity(true)



  val NewFranchiseName          = "NEW FRANCHISE NAME"
  val NewBusinessFranchise      = BusinessFranchiseYes(NewFranchiseName)
  val NewInvolvedInOtherDetails = "NEW INVOLVED"
  val NewInvolvedInOther         = InvolvedInOtherYes(NewInvolvedInOtherDetails)
  val NewBusinessTurnover       = ExpectedBusinessTurnover.Second
  val NewAMLSTurnover           = ExpectedAMLSTurnover.Second
  val NewTransactionRecord      = TransactionRecordNo
  val NewCustomersOutsideUK     = CustomersOutsideUK(None)
  val NewNCARegistered          = NCARegistered(false)
  val NewAccountantForAMLSRegulations = AccountantForAMLSRegulations(false)
  val NewRiskAssessment         = RiskAssessmentPolicyNo
  val NewIdentifySuspiciousActivity  = IdentifySuspiciousActivity(true)
  val NewWhoIsYourAccountant = WhoIsYourAccountant("newName", Some("newTradingName"),
    UkAccountantsAddress("98E", "Building1", Some("street1"), Some("road1"), "NE27 0QQ"),
    AccountantDoesAlsoDealWithTax("refer"))

    val completeJson = Json.obj(
      "involvedInOther" -> true,
      "details" -> DefaultInvolvedInOtherDetails,
      "expectedBusinessTurnover" -> "01",
      "expectedAMLSTurnover" -> "01",
      "businessFranchise" -> true,
      "franchiseName" -> DefaultFranchiseName,
      "isRecorded" -> true,
      "transactions" -> Seq("01", "03"),
      "digitalSoftwareName" -> DefaultSoftwareName,
      "isOutside" -> true,
      "countries" ->Json.arr("GB"),
      "ncaRegistered" -> true,
      "accountantForAMLSRegulations" -> true,
      "hasWrittenGuidance" -> true,
      "hasPolicy" -> true,
      "riskassessments" -> Seq("01"),
      "employeeCount" -> "5",
      "employeeCountAMLSSupervision" -> "4",
      "accountantsName" -> "Accountant's name",
      "accountantsTradingName" -> "Accountant's trading name",
      "accountantsAddressLine1" -> "address1",
      "accountantsAddressLine2" -> "address2",
      "accountantsAddressLine3" -> "address3",
      "accountantsAddressLine4" -> "address4",
      "accountantsAddressPostCode" -> "POSTCODE",
      "doesAccountantAlsoDealWithTax" -> true,
      "accountantsReference" -> "11Character"
    )

    val completeModel = BusinessActivities(involvedInOther = Some(DefaultInvolvedInOther),
                                           expectedBusinessTurnover = Some(DefaultBusinessTurnover),
                                           expectedAMLSTurnover = Some(DefaultAMLSTurnover) ,
                                           businessFranchise = Some(DefaultBusinessFranchise),
                                           transactionRecord = Some(DefaultTransactionRecord),
                                           customersOutsideUK = Some(DefaultCustomersOutsideUK),
                                           ncaRegistered = Some(DefaultNCARegistered),
                                           accountantForAMLSRegulations = Some(DefaultAccountantForAMLSRegulations),
                                           identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true)), // add test
                                           riskAssessmentPolicy = Some(DefaultRiskAssessments),
                                           howManyEmployees = Some(HowManyEmployees("5", "4")), // add test
                                           whoIsYourAccountant = Some(DefaultWhoIsYourAccountant)
                                          )

      val partialModel = BusinessActivities(businessFranchise = Some(DefaultBusinessFranchise))


  "BusinessActivities" must {
    "Serialise as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[BusinessActivities] must be(completeModel)
    }
  }

  "isComplete" must {
    "return false when the model is incomplete" in {
      partialModel.isComplete must be(false)
    }
    "return true when the model is complete" in {
      completeModel.isComplete must be(true)
    }
  }

  "Partially complete BusinessActivities" must {

    val partialJson = Json.obj(
      "businessFranchise" -> true,
      "franchiseName"     -> DefaultFranchiseName
    )

    "Serialise as expected" in {
      Json.toJson(partialModel) mustBe partialJson
    }

    "Deserialise as expected" in {
      partialJson.as[BusinessActivities] mustBe partialModel
    }
  }

  "None" must {

    val initial: Option[BusinessActivities] = None

    "Merged with InvolvedInOther" in {
      val result = initial.involvedInOther(NewInvolvedInOther)
      result must be (BusinessActivities(Some(NewInvolvedInOther), hasChanged = true))
    }

    "Merged with ExcpectedBusinessTurnover" in {
      val result = initial.expectedBusinessTurnover(NewBusinessTurnover)
      result must be (BusinessActivities(None, Some(NewBusinessTurnover), hasChanged = true))
    }

    "Merged with ExpectedAMLSTurnover" in {
      val result = initial.expectedAMLSTurnover(NewAMLSTurnover)
      result must be (BusinessActivities(None, None, Some(NewAMLSTurnover), hasChanged = true))
    }

    "Merged with BusinessFranchise" in {
      val result = initial.businessFranchise(NewBusinessFranchise)
      result must be (BusinessActivities(None, None, None, Some(NewBusinessFranchise), hasChanged = true))
    }

    "Merged with TransactionRecord" in {
      val result = initial.transactionRecord(NewTransactionRecord)
      result must be (BusinessActivities(None, None, None, None, Some(NewTransactionRecord), hasChanged = true))
    }

    "Merged with CustomersOutsideUK" in {
      val result = initial.customersOutsideUK(NewCustomersOutsideUK)
      result must be (BusinessActivities(None, None, None, None, None, Some(NewCustomersOutsideUK), hasChanged = true))
    }

    "Merged with ncaRegistered" in {
      val result = initial.ncaRegistered(NewNCARegistered)
      result must be (BusinessActivities(None, None, None, None, None, None, Some(NewNCARegistered), hasChanged = true))
    }

    "Merged with accountantForAMLSRegulations" in {
      val result = initial.accountantForAMLSRegulations(NewAccountantForAMLSRegulations)
      result must be (BusinessActivities(None, None, None, None, None, None, None, Some(NewAccountantForAMLSRegulations), hasChanged = true))
    }


    "Merged with RiskAssesment" in {
      val result = initial.riskAssessmentPolicy(NewRiskAssessment)
      result must be (BusinessActivities(riskAssessmentPolicy = Some(NewRiskAssessment), hasChanged = true))
    }

    "Merged with IdentifySuspiciousActivity" in {
      val result = initial.identifySuspiciousActivity(NewIdentifySuspiciousActivity)
      result must be (BusinessActivities(identifySuspiciousActivity = Some(NewIdentifySuspiciousActivity), hasChanged = true))
    }

    "Merged with WhoIsYourAccountant" in {
      val result = initial.whoIsYourAccountant(NewWhoIsYourAccountant)
      result must be (BusinessActivities(whoIsYourAccountant = Some(NewWhoIsYourAccountant), hasChanged = true))
    }

  }


  "ASP class" when {
    "involvedInOther value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.involvedInOther(DefaultInvolvedInOther)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.involvedInOther(InvolvedInOtherNo)
          res.hasChanged must be(true)
          res.involvedInOther must be(Some(InvolvedInOtherNo))
        }
      }
    }

    "expectedBusinessTurnover value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.expectedBusinessTurnover(DefaultBusinessTurnover)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.expectedBusinessTurnover(NewBusinessTurnover)
          res.hasChanged must be(true)
          res.expectedBusinessTurnover must be(Some(NewBusinessTurnover))
        }
      }
    }

    "expectedAMLSTurnover value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.expectedAMLSTurnover(DefaultAMLSTurnover)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.expectedAMLSTurnover(NewAMLSTurnover)
          res.hasChanged must be(true)
          res.expectedAMLSTurnover must be(Some(NewAMLSTurnover))
        }
      }
    }

    "businessFranchise value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.businessFranchise(DefaultBusinessFranchise)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.businessFranchise(NewBusinessFranchise)
          res.hasChanged must be(true)
          res.businessFranchise must be(Some(NewBusinessFranchise))
        }
      }
    }

    "transactionRecord value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.transactionRecord(DefaultTransactionRecord)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.transactionRecord(NewTransactionRecord)
          res.hasChanged must be(true)
          res.transactionRecord must be(Some(NewTransactionRecord))
        }
      }
    }

    "customersOutsideUK value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.customersOutsideUK(DefaultCustomersOutsideUK)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.customersOutsideUK(NewCustomersOutsideUK)
          res.hasChanged must be(true)
          res.customersOutsideUK must be(Some(NewCustomersOutsideUK))
        }
      }
    }

    "ncaRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.ncaRegistered(DefaultNCARegistered)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.ncaRegistered(NewNCARegistered)
          res.hasChanged must be(true)
          res.ncaRegistered must be(Some(NewNCARegistered))
        }
      }
    }

    "accountantForAMLSRegulations value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.accountantForAMLSRegulations(DefaultAccountantForAMLSRegulations)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.accountantForAMLSRegulations(NewAccountantForAMLSRegulations)
          res.hasChanged must be(true)
          res.accountantForAMLSRegulations must be(Some(NewAccountantForAMLSRegulations))
        }
      }
    }

    "riskAssessmentPolicy value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.riskAssessmentPolicy(DefaultRiskAssessments)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.riskAssessmentPolicy(NewRiskAssessment)
          res.hasChanged must be(true)
          res.riskAssessmentPolicy must be(Some(NewRiskAssessment))
        }
      }
    }

    "whoIsYourAccountant value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.whoIsYourAccountant(DefaultWhoIsYourAccountant)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.whoIsYourAccountant(NewWhoIsYourAccountant)
          res.hasChanged must be(true)
          res.whoIsYourAccountant must be(Some(NewWhoIsYourAccountant))
        }
      }
    }
  }
}
