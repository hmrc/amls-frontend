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

package utils.tradingpremises

import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Actions
import utils.DateHelper

import javax.inject.Inject

class CheckYourAnswersHelper @Inject() () {

  def createSummaryList(
    tradingPremises: TradingPremises,
    index: Int,
    isMsb: Boolean,
    hasOneService: Boolean,
    hasOneMsbService: Boolean
  )(implicit messages: Messages): SummaryList = {

    val rows: Seq[SummaryListRow] =
      Seq(
        businessStructureRow(tradingPremises, index),
        agentCompanyDetailsRow(tradingPremises, index)
      ).flatten ++
        agentPersonalDetailsRows(tradingPremises, index).getOrElse(Nil) ++
        Seq(
          agentPartnershipRow(tradingPremises, index),
          companyRegistrationNumberRow(tradingPremises, index)
        ).flatten ++
        tradingPremises.yourTradingPremises.fold(Seq.empty[SummaryListRow])(yta =>
          tradingPremisesRows(yta, index).getOrElse(Seq.empty[SummaryListRow])
        ) ++
        Seq(
          businessActivitiesRow(tradingPremises, index, hasOneService),
          msbServicesRow(tradingPremises, index, hasOneMsbService)
        ).flatten

    if (isMsb) {
      SummaryList(Seq(agentPremisesRow(tradingPremises, index)).flatten ++ rows)
    } else {
      SummaryList(rows)
    }
  }

  private def agentPremisesRow(tradingPremises: TradingPremises, index: Int)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    tradingPremises.registeringAgentPremises.map { agentPremises =>
      row(
        "tradingpremises.agent.premises.title",
        booleanToLabel(agentPremises.agentPremises),
        editAction(
          controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.agentPremRgstr",
          "tradingpremisessummarywho-edit"
        )
      )
    }

  private def businessStructureRow(tradingPremises: TradingPremises, index: Int)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    tradingPremises.businessStructure.map { structure =>
      row(
        "tradingpremises.businessStructure.title",
        messages(structure.message),
        editAction(
          controllers.tradingpremises.routes.BusinessStructureController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.agentBusType",
          "tradingpremisesbusinessstructure-edit"
        )
      )
    }

  private def agentCompanyDetailsRow(tradingPremises: TradingPremises, index: Int)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    tradingPremises.agentCompanyDetails.map { details =>
      row(
        "tradingpremises.youragent.company.name",
        details.agentCompanyName,
        editAction(
          controllers.tradingpremises.routes.AgentCompanyDetailsController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.agentCompDtls",
          "tradingpremisesagentcompanyname-edit"
        )
      )
    }

  private def agentPersonalDetailsRows(tradingPremises: TradingPremises, index: Int)(implicit
    messages: Messages
  ): Option[Seq[SummaryListRow]] =
    tradingPremises.agentName.map { name =>
      Seq(
        row(
          "tradingpremises.agentname.name.title",
          name.agentName,
          editAction(
            controllers.tradingpremises.routes.AgentNameController.get(index, true).url,
            "tradingpremises.checkYourAnswers.change.agentName",
            "tradingpremisesagentnametitle-edit"
          )
        )
      ) ++ name.agentDateOfBirth.fold(Seq.empty[SummaryListRow])(dob =>
        Seq(
          row(
            "tradingpremises.agentname.dob.title",
            DateHelper.formatDate(dob),
            editAction(
              controllers.tradingpremises.routes.AgentNameController.get(index, true).url,
              "tradingpremises.checkYourAnswers.change.agentDOB",
              "tradingpremisesagentdobtitle-edit"
            )
          )
        )
      )
    }

  private def agentPartnershipRow(tradingPremises: TradingPremises, index: Int)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    tradingPremises.agentPartnership.map { partnership =>
      row(
        "tradingpremises.agentpartnership.title",
        partnership.agentPartnership,
        editAction(
          controllers.tradingpremises.routes.AgentPartnershipController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.agentPrtnrs",
          "tradingpremisesagentpartnershiptitle-edit"
        )
      )
    }

  private def companyRegistrationNumberRow(tradingPremises: TradingPremises, index: Int)(implicit messages: Messages) =
    for {
      details   <- tradingPremises.agentCompanyDetails
      regNumber <- details.companyRegistrationNumber
    } yield row(
      "tradingpremises.youragent.crn",
      regNumber,
      editAction(
        controllers.tradingpremises.routes.AgentCompanyDetailsController.get(index, true).url,
        "tradingpremises.checkYourAnswers.change.agentCompDtls",
        "tradingpremisesyouragentcrn-edit"
      )
    )

  private def tradingPremisesRows(yta: YourTradingPremises, index: Int)(implicit
    messages: Messages
  ): Option[Seq[SummaryListRow]] =
    for {
      startDate     <- yta.startDate
      isResidential <- yta.isResidential
      name           = yta.tradingName
      address        = yta.tradingPremisesAddress
    } yield Seq(
      SummaryListRow(
        Key(Text(messages("tradingpremises.yourtradingpremises.title"))),
        nameAndAddressToLines(name, address),
        actions = editAction(
          controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.tradingPremDtls",
          "tradingpremisesdetails-edit"
        )
      ),
      row(
        "tradingpremises.startDate.title",
        DateHelper.formatDate(startDate),
        editAction(
          controllers.tradingpremises.routes.ActivityStartDateController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.tradingPremFrom",
          "tradingprmisedsummarystartdate-edit"
        )
      ),
      row(
        "tradingpremises.isResidential.title",
        booleanToLabel(isResidential),
        editAction(
          controllers.tradingpremises.routes.IsResidentialController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.resAddr",
          "tradingpremisessummaryresidential-edit"
        )
      )
    )

  private def businessActivitiesRow(tradingPremises: TradingPremises, index: Int, hasOneService: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    tradingPremises.whatDoesYourBusinessDoAtThisAddress.map { ba =>
      val message: Value = ba.activities.toList match {
        case activity :: Nil =>
          Value(Text(messages(s"businessmatching.registerservices.servicename.lbl.${activity.value}")))
        case activities      =>
          toBulletList(activities.sortBy(_.toString).map(_.value), "businessmatching.registerservices.servicename.lbl")
      }

      val changeLinkOpt: Option[Actions] = if (hasOneService) {
        None
      } else {
        editAction(
          controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.srvsPrvdd",
          "tradingpremisedsummaryservices-edit"
        )
      }

      SummaryListRow(
        Key(Text(messages("tradingpremises.whatdoesyourbusinessdo.title"))),
        message,
        actions = changeLinkOpt
      )
    }
  private def msbServicesRow(tradingPremises: TradingPremises, index: Int, hasOneMsbService: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    tradingPremises.msbServices.map { msb =>
      val message: Value = msb.services.toList match {
        case service :: Nil => Value(Text(messages(s"msb.services.list.lbl.${service.value}")))
        case services       =>
          toBulletList(services.sortBy(_.toString).map(_.value), "msb.services.list.lbl")
      }

      val changeLinkOpt: Option[Actions] = if (hasOneMsbService) {
        None
      } else {
        editAction(
          controllers.tradingpremises.routes.MSBServicesController.get(index, true).url,
          "tradingpremises.checkYourAnswers.change.MSBOnPrem",
          "tradingpremisesmsbservices-edit"
        )
      }

      SummaryListRow(
        Key(Text(messages("tradingpremises.msb.services.title"))),
        message,
        actions = changeLinkOpt
      )
    }

  private def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
    messages("lbl.yes")
  } else {
    messages("lbl.no")
  }

  private def row(title: String, label: String, actions: Option[Actions])(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      Key(Text(messages(title))),
      Value(Text(label)),
      actions = actions
    )

  private def editAction(route: String, hiddenText: String, id: String)(implicit messages: Messages): Option[Actions] =
    Some(
      Actions(
        items = Seq(
          ActionItem(
            route,
            Text(messages("button.edit")),
            visuallyHiddenText = Some(messages(hiddenText)),
            attributes = Map("id" -> id)
          )
        )
      )
    )

  private def toBulletList[A](coll: Seq[A], messagePrefix: String)(implicit messages: Messages): Value = Value(
    HtmlContent(
      Html(
        "<ul class=\"govuk-list govuk-list--bullet\">" +
          coll.map { x =>
            s"<li>${messages(s"$messagePrefix.$x")}</li>"
          }.mkString +
          "</ul>"
      )
    )
  )

  private def nameAndAddressToLines(name: String, address: Address): Value =
    Value(
      HtmlContent(
        Html(
          "<ul class=\"govuk-list\">" +
            s"<li>$name</li>" +
            address.toLines.map { line =>
              s"""<li>$line<li>"""
            }.mkString
            + "</ul>"
        )
      )
    )
}
