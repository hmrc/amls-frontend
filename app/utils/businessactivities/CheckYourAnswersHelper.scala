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

import models.businessactivities.TransactionTypes.DigitalSoftware
import models.businessactivities._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.CheckYourAnswersHelperFunctions

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()() extends CheckYourAnswersHelperFunctions {

  def createSummaryList(businessActivities: BusinessActivities,
                        needsAccountancyQuestions: Boolean)(implicit messages: Messages): SummaryList = {

    val rows =
      involvedInOthersRows(businessActivities).getOrElse(Nil) ++
        Seq(
          expectedBusinessTurnoverRow(businessActivities),
          expectedAMLSTurnoverRow(businessActivities)
        ).flatten ++
        businessFranchiseRows(businessActivities).getOrElse(Nil) ++
        howManyEmployeesRows(businessActivities).getOrElse(Nil) ++
        Seq(transactionRecordRow(businessActivities)).flatten ++
        transactionRecordTypesRows(businessActivities).getOrElse(Nil) ++
        Seq(
          identifySuspiciousActivityRow(businessActivities),
          ncaRegisteredRow(businessActivities)
        ).flatten ++
        riskAssessmentPolicyRows(businessActivities).getOrElse(Nil)

    if (needsAccountancyQuestions) {
      SummaryList(rows ++ accountancyRows(businessActivities).getOrElse(Nil))
    } else {
      SummaryList(rows)
    }
  }
  private def involvedInOthersRows(businessActivities: BusinessActivities)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    val yesNoEditAction = editAction(
      controllers.businessactivities.routes.InvolvedInOtherController.get(true).url,
      "involvedinother-edit"
    )

    businessActivities.involvedInOther.map {
      case InvolvedInOtherNo => Seq(
        row(
          "businessactivities.confirm-activities.title",
          booleanToLabel(false),
          yesNoEditAction
        )
      )
      case InvolvedInOtherYes(details) => Seq(
        row(
          "businessactivities.confirm-activities.title",
          booleanToLabel(true),
          yesNoEditAction
        ),
        row(
          "businessactivities.confirm-activities.lbl.details",
          details,
          editAction(
            controllers.businessactivities.routes.InvolvedInOtherController.get(true).url, "involvedinotherdetails-edit"
          )
        )
      )
    }
  }
  private def expectedBusinessTurnoverRow(businessActivities: BusinessActivities)(implicit messages: Messages): Option[SummaryListRow] = {

    businessActivities.expectedBusinessTurnover.map { ebt =>
      row(
        "businessactivities.business-turnover.title",
        messages(s"businessactivities.turnover.lbl.${ebt.value}"),
        editAction(
          controllers.businessactivities.routes.ExpectedBusinessTurnoverController.get(true).url,
          "expectedbusinessturnover-edit"
        )
      )
    }
  }

  private def expectedAMLSTurnoverRow(businessActivities: BusinessActivities)(implicit messages: Messages): Option[SummaryListRow] = {

    businessActivities.expectedAMLSTurnover.map { et =>
      row(
        "businessactivities.turnover.heading",
        messages(s"businessactivities.business-turnover.lbl.${et.value}"),
        editAction(
          controllers.businessactivities.routes.ExpectedAMLSTurnoverController.get(true).url,
          "expectedamlsturnover-edit"
        )
      )
    }
  }

  private def businessFranchiseRows(businessActivities: BusinessActivities)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def yesNoRow(bool: Boolean): SummaryListRow = {
      row(
        "businessactivities.businessfranchise.title",
        booleanToLabel(bool),
        editAction(
          controllers.businessactivities.routes.BusinessFranchiseController.get(true).url,
          "businessfranchise-edit"
        )
      )
    }

    businessActivities.businessFranchise.map {
      case BusinessFranchiseNo => Seq(yesNoRow(false))
      case BusinessFranchiseYes(name) => Seq(
        yesNoRow(true),
        row(
          "businessactivities.businessfranchise.lbl.franchisename",
          name,
          editAction(
            controllers.businessactivities.routes.BusinessFranchiseController.get(true).url,
            "businessfranchisename-edit"
          )
        )
      )
    }
  }

  private def howManyEmployeesRows(businessActivities: BusinessActivities)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    businessActivities.howManyEmployees.map { hme =>
      (hme.employeeCount, hme.employeeCountAMLSSupervision) match {
        case (Some(employeeCount), Some(amlsCount)) => Seq(
          row(
            "businessactivities.employees.line2.cya",
            amlsCount,
            editAction(
              controllers.businessactivities.routes.EmployeeCountAMLSSupervisionController.get(true).url,
              "employeescountline2-edit"
            )
          ),
          row(
            "businessactivities.employees.line1.cya",
            employeeCount,
            editAction(
              controllers.businessactivities.routes.HowManyEmployeesController.get(true).url,
              "employeescountline1-edit"
            )
          )
        )
        case _ => Nil
      }
    }
  }

  private def transactionRecordRow(businessActivities: BusinessActivities)(implicit messages: Messages): Option[SummaryListRow] = {

    businessActivities.transactionRecord.map { record =>
      row(
        "businessactivities.keep.customer.records.title",
        booleanToLabel(record),
        editAction(
          controllers.businessactivities.routes.TransactionRecordController.get(true).url,
          "keeprecords-edit"
        )
      )
    }
  }

  private def transactionRecordTypesRows(businessActivities: BusinessActivities)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def softwareRow(types: TransactionTypes): Option[SummaryListRow] = types.types.find(_.isInstanceOf[DigitalSoftware]) map {
      case DigitalSoftware(name) =>
        row(
          "businessactivities.name.software.pkg.lbl",
          name,
          actions = editAction(
            controllers.businessactivities.routes.TransactionTypesController.get(true).url,
            "software-edit"
          )
        )
    }

    businessActivities.transactionRecordTypes.map { transactionTypes =>

      Seq(
        getValueForRow[TransactionType](
          transactionTypes.types, x => messages(s"businessactivities.transactiontype.lbl.${x.value}")
        ).map { value =>
          SummaryListRow(
            Key(Text(messages("businessactivities.do.keep.records"))),
            value,
            actions = editAction(
              controllers.businessactivities.routes.TransactionTypesController.get(true).url,
              "keeprecordtypes-edit"
            )
          )
        },
        softwareRow(transactionTypes)
      ).flatten
    }
  }

  private def identifySuspiciousActivityRow(businessActivities: BusinessActivities)(implicit messages: Messages): Option[SummaryListRow] = {

    businessActivities.identifySuspiciousActivity.map { suspiciousActivity =>
      row(
        "businessactivities.identify-suspicious-activity.title",
        booleanToLabel(suspiciousActivity.hasWrittenGuidance),
        editAction(
          controllers.businessactivities.routes.IdentifySuspiciousActivityController.get(true).url,
          "suspiciousactivity-edit"
        )
      )
    }
  }

  private def ncaRegisteredRow(businessActivities: BusinessActivities)(implicit messages: Messages): Option[SummaryListRow] = {

    businessActivities.ncaRegistered.map { answer =>
      row(
        "businessactivities.ncaRegistered.title",
        booleanToLabel(answer.ncaRegistered),
        editAction(
          controllers.businessactivities.routes.NCARegisteredController.get(true).url,
          "ncaregistered-edit"
        )
      )
    }
  }

  private def riskAssessmentPolicyRows(businessActivities: BusinessActivities)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    businessActivities.riskAssessmentPolicy map {
      case RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), _) =>
        Seq(
          row(
            "businessactivities.riskassessment.policy.title",
            booleanToLabel(false),
            editAction(
              controllers.businessactivities.routes.RiskAssessmentController.get(true).url,
              "riskassessment-edit"
            )
          )
        )
      case RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(types)) =>
        Seq(
          Some(
            row(
              "businessactivities.riskassessment.policy.title",
              booleanToLabel(true),
              editAction(
                controllers.businessactivities.routes.RiskAssessmentController.get(true).url,
                "riskassessment-edit"
              )
            )
          ),
          getValueForRow[RiskAssessmentType](
            types, x => messages(s"businessactivities.RiskAssessmentType.lbl.${x.value}")
          ).map { value =>
            SummaryListRow(
              Key(Text(messages("businessactivities.document.riskassessment.policy.title"))),
              value,
              actions = editAction(
                controllers.businessactivities.routes.DocumentRiskAssessmentController.get(true).url,
                "documentriskassessment-edit"
              )
            )
          }
        ).flatten
    }
  }

  private def accountancyRows(activities: BusinessActivities)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def booleanRow(hasAccountant: Boolean): SummaryListRow = {
      row(
        "businessactivities.accountantForAMLSRegulations.title",
        booleanToLabel(hasAccountant),
        editAction(
          controllers.businessactivities.routes.AccountantForAMLSRegulationsController.get(true).url,
          "accountantforamlsregulations-edit"
        )
      )
    }

    def makeNameRow(name: WhoIsYourAccountantName) = {
      SummaryListRow(
        Key(Text(messages("businessactivities.whoisyouraccountant.title"))),
        nameValues(name),
        actions = editAction(
          controllers.businessactivities.routes.WhoIsYourAccountantNameController.get(true).url,
          "whoisyouraccountant-edit"
        )
      )
    }

    def makeIsUKRow(uk: WhoIsYourAccountantIsUk, name: WhoIsYourAccountantName) = {
      SummaryListRow(
        Key(Text(messages("businessactivities.whoisyouraccountant.location.header", name.accountantsName))),
        Value(Text(booleanToLabel(uk.isUk))),
        actions = editAction(
          controllers.businessactivities.routes.WhoIsYourAccountantIsUkController.get(true).url,
          "accountantisuk-edit"
        )
      )
    }

    def makeAddressRow(address: AccountantsAddress, name: WhoIsYourAccountantName) = {
      SummaryListRow(
        Key(Text(messages("businessactivities.whoisyouraccountant.address.header", name.accountantsName))),
        addressToLines(address.toLines),
        actions = editAction(
          if (address.isUk) {
            controllers.businessactivities.routes.WhoIsYourAccountantUkAddressController.get(true).url
          } else {
            controllers.businessactivities.routes.WhoIsYourAccountantNonUkAddressController.get(true).url
          },
          "accountantaddress-edit"
        )
      )
    }

    def makeTaxMattersRow(matters: TaxMatters, name: WhoIsYourAccountantName) = {
      SummaryListRow(
        Key(Text(messages("businessactivities.tax.matters.summary.title", name.accountantsName))),
        Value(Text(booleanToLabel(matters.manageYourTaxAffairs))),
        actions = editAction(
          controllers.businessactivities.routes.TaxMattersController.get(true).url,
          "taxmatters-edit"
        )
      )
    }

    def nameValues(names: WhoIsYourAccountantName): Value = {
      val name = {
        (names.accountantsName, names.accountantsTradingName) match {
          case (name, Some(tradeName)) =>
            s"""<ul class="govuk-list"><li>Name: $name</li>""" +
            s"""<li>Trading name: $tradeName</li></ul>"""
          case (name, None) => name
        }
      }
      Value(HtmlContent(name))
    }

    activities.accountantForAMLSRegulations match {
      case Some(AccountantForAMLSRegulations(true)) =>
        for {
          accountant <- activities.whoIsYourAccountant
          name <- accountant.names
          isUK <- accountant.isUk
          address <- accountant.address
          taxMatters <- activities.taxMatters
        } yield {
          Seq(
            booleanRow(true),
            makeNameRow(name),
            makeIsUKRow(isUK, name),
            makeAddressRow(address, name),
            makeTaxMattersRow(taxMatters, name)
          )
        }
      case Some(AccountantForAMLSRegulations(false)) =>
        Some(Seq(booleanRow(false)))
      case _ => None
    }
  }

  private def getValueForRow[A](types: Set[A], f: A => String)(implicit messages: Messages): Option[Value] = types match {
    case types if types.size > 1 =>
      Some(
        toBulletList(types.map(f).toSeq)
      )
    case types if types.size == 1 =>
      Some(Value(Text(messages(f(types.head)))))
    case _ => None
  }
}
