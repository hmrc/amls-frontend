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

package utils.businessactivities

import models.Country
import models.businessactivities.TransactionTypes.{DigitalSoftware, Paper}
import models.businessactivities._
import models.businessmatching.BusinessActivity
import models.businessmatching.BusinessActivity._
import org.scalatest.Assertion
import play.api.test.Injecting
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import utils.AmlsSpec

class CheckYourAnswersHelperSpec extends AmlsSpec with Injecting {

  lazy val cyaHelper: CheckYourAnswersHelper = inject[CheckYourAnswersHelper]

  val involvedInOthersIndex             = 0
  val involvedInOthersDetailIndex       = 1
  val expectedBusinessTurnoverIndex     = 2
  val expectedAMLSTurnoverIndex         = 3
  val businessFranchiseIndex            = 4
  val businessFranchiseNameIndex        = 5
  val amlsEmployeeCountIndex            = 6
  val employeeCountIndex                = 7
  val transactionRecordIndex            = 8
  val transactionTypesIndex             = 9
  val softwareNameIndex                 = 10
  val suspiciousActivityIndex           = 11
  val ncaRegisteredIndex                = 12
  val riskAssessmentPolicyIndex         = 13
  val riskAssessmentTypesIndex          = 14
  val accountantForAMLSRegulationsIndex = 15
  val accountantNameIndex               = 16
  val accountantIsUKIndex               = 17
  val accountantAddressIndex            = 18
  val taxMattersIndex                   = 19

  val completeModel: BusinessActivities = BusinessActivities(
    involvedInOther = Some(BusinessActivitiesValues.defaultInvolvedInOther),
    expectedBusinessTurnover = Some(BusinessActivitiesValues.defaultBusinessTurnover),
    expectedAMLSTurnover = Some(BusinessActivitiesValues.defaultAMLSTurnover),
    businessFranchise = Some(BusinessActivitiesValues.defaultBusinessFranchise),
    transactionRecord = Some(BusinessActivitiesValues.defaultTransactionRecord),
    customersOutsideUK = Some(BusinessActivitiesValues.defaultCustomersOutsideUK),
    ncaRegistered = Some(BusinessActivitiesValues.defaultNCARegistered),
    accountantForAMLSRegulations = Some(BusinessActivitiesValues.defaultAccountantForAMLSRegulations),
    riskAssessmentPolicy = Some(BusinessActivitiesValues.defaultRiskAssessments),
    howManyEmployees = Some(BusinessActivitiesValues.defaultHowManyEmployees),
    identifySuspiciousActivity = Some(BusinessActivitiesValues.defaultIdentifySuspiciousActivity),
    whoIsYourAccountant = Some(BusinessActivitiesValues.defaultWhoIsYourAccountant),
    taxMatters = Some(BusinessActivitiesValues.defaultTaxMatters),
    transactionRecordTypes = Some(BusinessActivitiesValues.defaultTransactionRecordTypes)
  )

  val businessMatchingActivitiesList: Set[BusinessActivity] = Set(
    ArtMarketParticipant,
    BillPaymentServices,
    EstateAgentBusinessService,
    HighValueDealing,
    MoneyServiceBusiness,
    TrustAndCompanyServices,
    TelephonePaymentService
  )

  def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
    val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

    changeLink.content.toString must include(messages("button.edit"))
    changeLink.href mustBe href
    changeLink.attributes("id") mustBe id
  }

  ".createSummaryList" must {

    val summaryListRows = cyaHelper
      .createSummaryList(
        completeModel,
        needsAccountancyQuestions = true
      )
      .rows

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String) = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(title)

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    "return the correct summary list row" which {

      "contains Involved In Others information" when {

        "yes no answer is present" in {

          assertRowMatches(
            involvedInOthersIndex,
            messages("businessactivities.confirm-activities.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.InvolvedInOtherController.get(true).url,
            "involvedinother-edit"
          )
        }
      }

      "contains Involved In Others Details" when {

        "review details are present" in {

          assertRowMatches(
            involvedInOthersDetailIndex,
            messages("businessactivities.confirm-activities.cya.lbl"),
            BusinessActivitiesValues.defaultInvolvedInOtherDetails,
            controllers.businessactivities.routes.InvolvedInOtherController.get(true).url,
            "involvedinotherdetails-edit"
          )
        }
      }

      "contains Expected Business Turnover" when {

        "business turnover is present" in {

          assertRowMatches(
            expectedBusinessTurnoverIndex,
            messages("businessactivities.business-turnover.cya"),
            messages(s"businessactivities.turnover.lbl.${BusinessActivitiesValues.defaultBusinessTurnover.value}"),
            controllers.businessactivities.routes.ExpectedBusinessTurnoverController.get(true).url,
            "expectedbusinessturnover-edit"
          )
        }
      }

      "contains Expected AMLS Turnover" in {

        val result = cyaHelper
          .createSummaryList(
            completeModel,
            needsAccountancyQuestions = true
          )
          .rows
          .lift(expectedAMLSTurnoverIndex)
          .getOrElse(fail(s"Row for index $expectedAMLSTurnoverIndex does not exist"))

        result.key.toString must include(
          messages(
            "businessactivities.turnover.heading",
            messages("businessactivities.registerservices.servicename.lbl.06")
          )
        )

        result.value.toString must include(
          messages(s"businessactivities.turnover.lbl.${BusinessActivitiesValues.defaultAMLSTurnover.value}")
        )

        checkChangeLink(
          result,
          controllers.businessactivities.routes.ExpectedAMLSTurnoverController.get(true).url,
          "expectedamlsturnover-edit"
        )
      }

      "contains Business Franchise Information" when {

        "franchise yes/no is present" in {

          assertRowMatches(
            businessFranchiseIndex,
            messages("businessactivities.businessfranchise.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.BusinessFranchiseController.get(true).url,
            "businessfranchise-edit"
          )
        }

        "franchise name is present" in {

          assertRowMatches(
            businessFranchiseNameIndex,
            messages("businessactivities.businessfranchise.lbl.franchisename"),
            BusinessActivitiesValues.defaultFranchiseName,
            controllers.businessactivities.routes.BusinessFranchiseController.get(true).url,
            "businessfranchisename-edit"
          )
        }
      }

      "contains AMLS Employee count Information" when {

        "AMLS employee count is present" in {

          assertRowMatches(
            amlsEmployeeCountIndex,
            messages("businessactivities.employees.line2.cya"),
            BusinessActivitiesValues.defaultHowManyEmployees.employeeCountAMLSSupervision.get,
            controllers.businessactivities.routes.EmployeeCountAMLSSupervisionController.get(true).url,
            "employeescountline2-edit"
          )
        }
      }

      "contains Employee count Information" when {

        "employee count is present" in {

          assertRowMatches(
            employeeCountIndex,
            messages("businessactivities.employees.line1.cya"),
            BusinessActivitiesValues.defaultHowManyEmployees.employeeCount.get,
            controllers.businessactivities.routes.HowManyEmployeesController.get(true).url,
            "employeescountline1-edit"
          )
        }
      }

      "contains Transaction record Information" when {

        "transaction record is present" in {

          assertRowMatches(
            transactionRecordIndex,
            messages("businessactivities.keep.customer.records.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.TransactionRecordController.get(true).url,
            "keeprecords-edit"
          )
        }
      }

      "contains Transaction types Information" when {

        "transaction types are present" in {

          assertRowMatches(
            transactionTypesIndex,
            messages("businessactivities.do.keep.records.cya"),
            "<ul class=\"govuk-list govuk-list--bullet\">" +
              BusinessActivitiesValues.defaultTransactionRecordTypes.types.map { x =>
                "<li>" + messages(s"businessactivities.transactiontype.lbl.${x.value}") + "</li>"
              }.mkString +
              "</ul>",
            controllers.businessactivities.routes.TransactionTypesController.get(true).url,
            "keeprecordtypes-edit"
          )
        }

        "software name is present" in {

          assertRowMatches(
            softwareNameIndex,
            messages("businessactivities.name.software.pkg.lbl"),
            BusinessActivitiesValues.defaultSoftwareName,
            controllers.businessactivities.routes.TransactionTypesController.get(true).url,
            "software-edit"
          )
        }
      }

      "contains Suspicious activity Information" when {

        "suspicious activity is present" in {

          assertRowMatches(
            suspiciousActivityIndex,
            messages("businessactivities.identify-suspicious-activity.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.IdentifySuspiciousActivityController.get(true).url,
            "suspiciousactivity-edit"
          )
        }
      }

      "contains NCA registered Information" when {

        "NCA registered is present" in {

          assertRowMatches(
            ncaRegisteredIndex,
            messages("businessactivities.ncaRegistered.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.NCARegisteredController.get(true).url,
            "ncaregistered-edit"
          )
        }
      }

      "contains risk assessment policy" when {

        "risk assessment policy is present" in {

          assertRowMatches(
            riskAssessmentPolicyIndex,
            messages("businessactivities.riskassessment.policy.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.RiskAssessmentController.get(true).url,
            "riskassessment-edit"
          )
        }
      }

      "contains risk assessment types" when {

        "there are multiple types" in {

          val types = Set[RiskAssessmentType](PaperBased, Digital)

          val result = cyaHelper
            .createSummaryList(
              completeModel.copy(
                riskAssessmentPolicy = Some(
                  RiskAssessmentPolicy(
                    RiskAssessmentHasPolicy(true),
                    RiskAssessmentTypes(types)
                  )
                )
              ),
              needsAccountancyQuestions = true
            )
            .rows
            .lift(riskAssessmentTypesIndex)
            .getOrElse(fail(s"Row for index $riskAssessmentTypesIndex does not exist"))

          result.key.toString must include(messages("businessactivities.document.riskassessment.policy.cya"))

          result.value.toString must include(
            "<ul class=\"govuk-list govuk-list--bullet\">" +
              types.map { x =>
                "<li>" + messages(s"businessactivities.RiskAssessmentType.lbl.${x.value}") + "</li>"
              }.mkString +
              "</ul>"
          )

          checkChangeLink(
            result,
            controllers.businessactivities.routes.DocumentRiskAssessmentController.get(true).url,
            "documentriskassessment-edit"
          )
        }

        "there is one type" in {

          assertRowMatches(
            riskAssessmentTypesIndex,
            messages("businessactivities.document.riskassessment.policy.cya"),
            messages(s"businessactivities.RiskAssessmentType.lbl.${PaperBased.value}"),
            controllers.businessactivities.routes.DocumentRiskAssessmentController.get(true).url,
            "documentriskassessment-edit"
          )
        }
      }

      "has accountancy rows enabled and has rows" when {

        val names = BusinessActivitiesValues.defaultWhoIsYourAccountant.names.get

        "accountant for AMLS is present" in {

          assertRowMatches(
            accountantForAMLSRegulationsIndex,
            messages("businessactivities.accountantForAMLSRegulations.title"),
            messages("lbl.yes"),
            controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get(true).url,
            "accountantforamlsregulations-edit"
          )
        }

        "accountant name is present" in {

          val name = "John Doe"

          val result = cyaHelper
            .createSummaryList(
              completeModel.copy(
                whoIsYourAccountant = Some(
                  BusinessActivitiesValues.defaultWhoIsYourAccountant.copy(
                    names = Some(WhoIsYourAccountantName(name, None))
                  )
                )
              ),
              needsAccountancyQuestions = true
            )
            .rows
            .lift(accountantNameIndex)
            .getOrElse(fail(s"Row for index $accountantNameIndex does not exist"))

          result.key.toString must include(messages("businessactivities.whoisyouraccountant.cya"))

          result.value.toString must include(name)

          checkChangeLink(
            result,
            controllers.businessactivities.routes.WhoIsYourAccountantNameController.get(true).url,
            "whoisyouraccountant-edit"
          )
        }

        "accountant name and trading name are present" in {

          assertRowMatches(
            accountantNameIndex,
            messages("businessactivities.whoisyouraccountant.cya"),
            s"""<ul class="govuk-list"><li>Name: ${names.accountantsName}</li>""" +
              s"""<li>Trading name: ${names.accountantsTradingName.get}</li></ul>""",
            controllers.businessactivities.routes.WhoIsYourAccountantNameController.get(true).url,
            "whoisyouraccountant-edit"
          )
        }

        "accountant name is UK is present" in {

          assertRowMatches(
            accountantIsUKIndex,
            messages("businessactivities.whoisyouraccountant.location.header", names.accountantsName),
            messages("lbl.yes"),
            controllers.businessactivities.routes.WhoIsYourAccountantIsUkController.get(true).url,
            "accountantisuk-edit"
          )
        }

        "accountant UK address is present" in {

          val address = BusinessActivitiesValues.defaultWhoIsYourAccountant.address.get

          assertRowMatches(
            accountantAddressIndex,
            messages("businessactivities.whoisyouraccountant.address.cya", names.accountantsName),
            "<ul class=\"govuk-list\">" +
              address.toLines.map { line =>
                s"""<li>$line<li>"""
              }.mkString
              + "</ul>",
            controllers.businessactivities.routes.WhoIsYourAccountantUkAddressController.get(true).url,
            "accountantaddress-edit"
          )
        }

        "accountant non-UK address is present" in {

          val address = NonUkAccountantsAddress(
            "address1",
            Some("address2"),
            Some("address3"),
            Some("address4"),
            Country("United States", "US")
          )

          val result = cyaHelper
            .createSummaryList(
              completeModel.copy(
                whoIsYourAccountant = Some(
                  BusinessActivitiesValues.defaultWhoIsYourAccountant.copy(
                    address = Some(address)
                  )
                )
              ),
              needsAccountancyQuestions = true
            )
            .rows
            .lift(accountantAddressIndex)
            .getOrElse(fail(s"Row for index $accountantAddressIndex does not exist"))

          result.key.toString must include(
            messages("businessactivities.whoisyouraccountant.address.cya", names.accountantsName)
          )

          result.value.toString must include(
            "<ul class=\"govuk-list\">" +
              address.toLines.map { line =>
                s"""<li>$line<li>"""
              }.mkString
              + "</ul>"
          )

          checkChangeLink(
            result,
            controllers.businessactivities.routes.WhoIsYourAccountantNonUkAddressController.get(true).url,
            "accountantaddress-edit"
          )
        }

        "tax matters is present" in {

          assertRowMatches(
            taxMattersIndex,
            messages("businessactivities.tax.matters.summary.title", names.accountantsName),
            messages("lbl.no"),
            controllers.businessactivities.routes.TaxMattersController.get(true).url,
            "taxmatters-edit"
          )
        }
      }

      "has no accountancy answers" when {

        "needsAccountancyQuestions is false" in {

          val result = cyaHelper.createSummaryList(completeModel, needsAccountancyQuestions = false)

          Seq(
            accountantForAMLSRegulationsIndex,
            accountantNameIndex,
            accountantIsUKIndex,
            accountantAddressIndex,
            taxMattersIndex
          ) foreach { i =>
            result.rows.lift(i) mustBe None
          }
        }
      }
    }

  }
}

object BusinessActivitiesValues {
  val defaultFranchiseName                = "DEFAULT FRANCHISE NAME"
  val defaultSoftwareName                 = "DEFAULT SOFTWARE"
  val defaultBusinessTurnover             = ExpectedBusinessTurnover.First
  val defaultAMLSTurnover                 = ExpectedAMLSTurnover.First
  val defaultInvolvedInOtherDetails       = "DEFAULT INVOLVED"
  val defaultInvolvedInOther              = InvolvedInOtherYes(defaultInvolvedInOtherDetails)
  val defaultBusinessFranchise            = BusinessFranchiseYes(defaultFranchiseName)
  val defaultTransactionRecord            = true
  val defaultTransactionRecordTypes       = TransactionTypes(Set(Paper, DigitalSoftware(defaultSoftwareName)))
  val defaultCustomersOutsideUK           = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))
  val defaultNCARegistered                = NCARegistered(true)
  val defaultAccountantForAMLSRegulations = AccountantForAMLSRegulations(true)
  val defaultRiskAssessments              = RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(Set(PaperBased)))
  val defaultHowManyEmployees             = HowManyEmployees(Some("5"), Some("4"))
  val defaultWhoIsYourAccountant          = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName("Accountant's name", Some("Accountant's trading name"))),
    Some(WhoIsYourAccountantIsUk(true)),
    Some(UkAccountantsAddress("address1", Some("address2"), Some("address3"), Some("address4"), "POSTCODE"))
  )
  val defaultIdentifySuspiciousActivity   = IdentifySuspiciousActivity(true)
  val defaultTaxMatters                   = TaxMatters(false)
}
