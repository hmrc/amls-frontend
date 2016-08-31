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
  val DefaultWhoIsYourAccountant = WhoIsYourAccountant(
    "Accountant's name",
    Some("Accountant's trading name"),
    UkAccountantsAddress("address1", "address2", Some("address3"), Some("address4"), "POSTCODE" )
  )
  val DefaultIdentifySuspiciousActivity  = IdentifySuspiciousActivity(true)
  val DefaultTaxMatters = TaxMatters(true)

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
  val NewWhoIsYourAccountant = WhoIsYourAccountant(
    "newName",
    Some("newTradingName"),
    UkAccountantsAddress("98E", "Building1", Some("street1"), Some("road1"), "NE27 0QQ")
  )
  val NewTaxMatters = TaxMatters(true)

  "BusinessActivities" must {

    val completeModel = BusinessActivities(
      involvedInOther = Some(DefaultInvolvedInOther),
      expectedBusinessTurnover = Some(DefaultBusinessTurnover),
      expectedAMLSTurnover = Some(DefaultAMLSTurnover) ,
      businessFranchise = Some(DefaultBusinessFranchise),
      transactionRecord = Some(DefaultTransactionRecord),
      customersOutsideUK = Some(DefaultCustomersOutsideUK),
      ncaRegistered = Some(DefaultNCARegistered),
      accountantForAMLSRegulations = Some(DefaultAccountantForAMLSRegulations),
      riskAssessmentPolicy = Some(DefaultRiskAssessments),
      identifySuspiciousActivity = Some(DefaultIdentifySuspiciousActivity),
      whoIsYourAccountant = Some(DefaultWhoIsYourAccountant),
      taxMatters = Some(DefaultTaxMatters)
    )

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
      "hasPolicy" -> true,
      "riskassessments" -> Seq("01"),
      "accountantsName" -> "Accountant's name",
      "accountantsTradingName" -> "Accountant's trading name",
      "accountantsAddressLine1" -> "address1",
      "accountantsAddressLine2" -> "address2",
      "accountantsAddressLine3" -> "address3",
      "accountantsAddressLine4" -> "address4",
      "accountantsAddressPostCode" -> "POSTCODE",
      "manageYourTaxAffairs" -> true,
      "hasWrittenGuidance" -> true
    )

    "Serialise as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }
 
    "Deserialise as expected" in {
      completeJson.as[BusinessActivities] must be(completeModel)
    }

  }

  "Partially complete BusinessActivities" must {

    val partialJson = Json.obj(
      "businessFranchise" -> true,
      "franchiseName"     -> DefaultFranchiseName
    )

    val partialModel = BusinessActivities(businessFranchise = Some(DefaultBusinessFranchise))

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
      result must be (BusinessActivities(Some(NewInvolvedInOther)))
    }

    "Merged with ExcpectedBusinessTurnover" in {
      val result = initial.expectedBusinessTurnover(NewBusinessTurnover)
      result must be (BusinessActivities(None, Some(NewBusinessTurnover)))
    }

    "Merged with ExpectedAMLSTurnover" in {
      val result = initial.expectedAMLSTurnover(NewAMLSTurnover)
      result must be (BusinessActivities(None, None, Some(NewAMLSTurnover)))
    }

    "Merged with BusinessFranchise" in {
      val result = initial.businessFranchise(NewBusinessFranchise)
      result must be (BusinessActivities(None, None, None, Some(NewBusinessFranchise)))
    }

    "Merged with TransactionRecord" in {
      val result = initial.transactionRecord(NewTransactionRecord)
      result must be (BusinessActivities(None, None, None, None, Some(NewTransactionRecord)))
    }

    "Merged with CustomersOutsideUK" in {
      val result = initial.customersOutsideUK(NewCustomersOutsideUK)
      result must be (BusinessActivities(None, None, None, None, None, Some(NewCustomersOutsideUK)))
    }

    "Merged with ncaRegistered" in {
      val result = initial.ncaRegistered(NewNCARegistered)
      result must be (BusinessActivities(None, None, None, None, None, None, Some(NewNCARegistered)))
    }

    "Merged with accountantForAMLSRegulations" in {
      val result = initial.accountantForAMLSRegulations(NewAccountantForAMLSRegulations)
      result must be (BusinessActivities(None, None, None, None, None, None, None, Some(NewAccountantForAMLSRegulations)))
    }

    "Merged with RiskAssesment" in {
      val result = initial.riskAssessmentspolicy(NewRiskAssessment)
      result must be (BusinessActivities(riskAssessmentPolicy = Some(NewRiskAssessment)))
    }

    "Merged with IdentifySuspiciousActivity" in {
      val result = initial.identifySuspiciousActivity(NewIdentifySuspiciousActivity)
      result must be (BusinessActivities(identifySuspiciousActivity = Some(NewIdentifySuspiciousActivity)))
    }

    "Merged with WhoIsYourAccountant" in {
      val result = initial.whoIsYourAccountant(NewWhoIsYourAccountant)
      result must be (BusinessActivities(whoIsYourAccountant = Some(NewWhoIsYourAccountant)))
    }

    "Merged with TaxMatters" in {
      val result = initial.taxMatters(NewTaxMatters)
      result must be (BusinessActivities(taxMatters = Some(NewTaxMatters)))
    }
  }

  "BusinessActivities" must {

    val initial = BusinessActivities(
      involvedInOther = Some(DefaultInvolvedInOther),
      expectedBusinessTurnover = Some(DefaultBusinessTurnover),
      expectedAMLSTurnover = Some(DefaultAMLSTurnover) ,
      businessFranchise = Some(DefaultBusinessFranchise),
      transactionRecord = Some(DefaultTransactionRecord),
      customersOutsideUK = Some(DefaultCustomersOutsideUK),
      ncaRegistered = Some(DefaultNCARegistered),
      accountantForAMLSRegulations = Some(DefaultAccountantForAMLSRegulations),
      identifySuspiciousActivity = Some(DefaultIdentifySuspiciousActivity),
      riskAssessmentPolicy = Some(DefaultRiskAssessments),
      whoIsYourAccountant = Some(DefaultWhoIsYourAccountant),
      taxMatters = Some(DefaultTaxMatters)
    )

    "involvedInOther" must {
      "return BusinessActivities object with correct involvedInOther set" in {
        val result = initial.involvedInOther(NewInvolvedInOther)
        result must be(initial.copy(involvedInOther = Some(NewInvolvedInOther)))
      }
    }

    "expectedBusinessTurnover" must {
      "return BusinessActivities object with correct expectedBusinessTurnover set" in {
        val result = initial.expectedBusinessTurnover(NewBusinessTurnover)
        result must be(initial.copy(expectedBusinessTurnover = Some(NewBusinessTurnover)))
      }
    }

    "expectedAMLSTurnover" must {
      "return BusinessActivities object with correct expectedAMLSTurnover set" in {
        val result = initial.expectedAMLSTurnover(NewAMLSTurnover)
        result must be(initial.copy(expectedAMLSTurnover = Some(NewAMLSTurnover)))
      }
    }

    "businessFranchise" must {
      "return BusinessActivities object with correct businessFranchise set" in {
        val result = initial.businessFranchise(NewBusinessFranchise)
        result must be(initial.copy(businessFranchise = Some(NewBusinessFranchise)))
      }
    }

    "transactionRecord" must {
      "return BusinessActivities object with correct transactionRecord set" in {
        val result = initial.transactionRecord(NewTransactionRecord)
        result must be(initial.copy(transactionRecord = Some(NewTransactionRecord)))
      }
    }

    "customersOutsideUK" must {
      "return BusinessActivities object with correct customersOutsideUK set" in {
        val result = initial.customersOutsideUK(NewCustomersOutsideUK)
        result must be(initial.copy(customersOutsideUK = Some(NewCustomersOutsideUK)))
      }
    }

    "ncaRegistered" must {
      "return BusinessActivities object with correct ncaRegistered set" in {
        val result = initial.ncaRegistered(NewNCARegistered)
        result must be(initial.copy(ncaRegistered = Some(NewNCARegistered)))
      }
    }

    "accountantForAMLSRegulations" must {
      "return BusinessActivities object with correct accountantForAMLSRegulations set" in {
        val result = initial.accountantForAMLSRegulations(NewAccountantForAMLSRegulations)
        result must be(initial.copy(accountantForAMLSRegulations = Some(NewAccountantForAMLSRegulations)))
      }
    }

    "RiskAssessment" must {
      "return BusinessActivities object with correct riskAssessmentPolicy set" in {
        val result = initial.riskAssessmentspolicy(NewRiskAssessment)
        result must be(initial.copy(riskAssessmentPolicy = Some(NewRiskAssessment)))
      }
    }

    "IdentifySuspiciousActivity" must {
      "return BusinessActivities object with correct identifySuspiciousActivity set" in {
        val result = initial.identifySuspiciousActivity(NewIdentifySuspiciousActivity)
        result must be(initial.copy(identifySuspiciousActivity = Some(NewIdentifySuspiciousActivity)))
      }
    }

    "WhoIsYourAccountant" must {
      "return BusinessActivities object with correct WhoIsYourAccountant set" in {
        val result = initial.whoIsYourAccountant(NewWhoIsYourAccountant)
        result must be(initial.copy(whoIsYourAccountant = Some(NewWhoIsYourAccountant)))
      }
    }

    "TaxMatters" must {
      "return BusinessActivities object with correct TaxMatters set" in {
        val result = initial.taxMatters(NewTaxMatters)
        result must be(initial.copy(taxMatters = Some(NewTaxMatters)))
      }
    }

  }
}
