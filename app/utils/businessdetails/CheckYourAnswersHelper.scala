/*
 * Copyright 2023 HM Revenue & Customs
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

package utils.businessdetails

import config.ApplicationConfig
import models.businessdetails._
import models.businessmatching._
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryList}
import utils.DateHelper
import views.html.components.{Button => SubmissionButton}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()(button: SubmissionButton, appConfig: ApplicationConfig) {

  def createSummaryList(businessDetails: BusinessDetails, showRegisteredForMLR: Boolean)(implicit messages: Messages): SummaryList = {

    val rows = Seq(
      businessCurrentlyRegisteredRow(showRegisteredForMLR, businessDetails),
      activityStartDateRow(businessDetails)
    ).flatten ++
      vatRegisteredRow(businessDetails).getOrElse(Nil) ++
      registeredOfficeRow(businessDetails).getOrElse(Nil) ++
      contactingYouRows(businessDetails).getOrElse(Nil) ++
      correspondenceAddressRows(businessDetails).getOrElse(Nil)

    SummaryList(rows)
  }

  private def businessCurrentlyRegisteredRow(showRegisteredForMLR: Boolean, businessMatching: BusinessDetails)(implicit messages: Messages): Option[SummaryListRow] = {

    val labelOpt = businessMatching.previouslyRegistered.map {
      case PreviouslyRegisteredNo => messages("lbl.no")
      case PreviouslyRegisteredYes(_) => messages("lbl.yes")
    }

    if (showRegisteredForMLR) {
      labelOpt map { label =>
        row(
          "businessdetails.registeredformlr.title",
          label,
          Some(Actions(
            items = Seq(ActionItem(
              controllers.businessdetails.routes.PreviouslyRegisteredController.get(true).url,
              Text(messages("button.edit")),
              attributes = Map("id" -> "businessdetailsregform-edit")
            ))
          ))
        )
      }
    } else None
  }

  private def activityStartDateRow(businessDetails: BusinessDetails)(implicit messages: Messages): Option[SummaryListRow] = {

    businessDetails.activityStartDate map { activityStartDate =>
      row(
        "businessdetails.activity.start.date.title",
        DateHelper.formatDate(activityStartDate.startDate),
        Some(Actions(
          items = Seq(ActionItem(
            controllers.businessdetails.routes.ActivityStartDateController.get(true).url,
            Text(messages("button.edit")),
            attributes = Map("id" -> "businessdetailsactivitystartdate-edit")
          ))
        ))
      )
    }
  }

  private def vatRegisteredRow(businessDetails: BusinessDetails)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    val vatAction = editAction(
      controllers.businessdetails.routes.VATRegisteredController.get(true).url,
      "businessdetailsregform-edit"
    )

    businessDetails.vatRegistered.map {
      case VATRegisteredNo => Seq(row("businessdetails.registeredforvat.title", messages("lbl.no"), vatAction))
      case VATRegisteredYes(vatNo) => Seq(
        row("businessdetails.registeredforvat.title", messages("lbl.yes"), vatAction),
        row("lbl.vat.reg.number", vatNo, None)
      )
    }
  }

  private def registeredOfficeRow(businessDetails: BusinessDetails)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    val yesNoEdit = editAction(
      controllers.businessdetails.routes.RegisteredOfficeIsUKController.get(true).url,
      "businessdetailsregisteredofficeisuk-edit"
    )

    businessDetails.registeredOffice.map {
      case ukAddress @ RegisteredOfficeUK(_, _, _, _, _, _) =>
        Seq(
          row(
            "businessdetails.registeredoffice.title",
            messages("lbl.yes"),
            yesNoEdit
          ),
          SummaryListRow(
            Key(
              Text(messages("businessdetails.registeredoffice.where.title"))
            ),
            addressToLines(ukAddress.toLines),
            actions = editAction(
              controllers.businessdetails.routes.RegisteredOfficeUKController.get(true).url,
              "businessdetailsregoffice-edit"
            )
          )
        )

      case nonUkAddress @ RegisteredOfficeNonUK(_, _, _, _, _, _) =>
        Seq(
          row(
            "businessdetails.registeredoffice.title",
            messages("lbl.no"),
            yesNoEdit
          ),
          SummaryListRow(
            Key(
              Text(messages("businessdetails.registeredoffice.where.title"))
            ),
            addressToLines(nonUkAddress.toLines),
            actions = editAction(
              controllers.businessdetails.routes.RegisteredOfficeNonUKController.get(true).url,
              "businessdetailsregoffice-edit"
            )
          )
        )
    }
  }

  private def contactingYouRows(businessDetails: BusinessDetails)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    for {
      contactingYou <- businessDetails.contactingYou
      email <- contactingYou.email
      phoneNumber <- contactingYou.phoneNumber
    } yield {
      Seq(
        row(
          "businessdetails.contactingyou.email.title",
          email,
          editAction(
            controllers.businessdetails.routes.BusinessEmailAddressController.get(true).url,
            "businessdetailscontactyou-edit"
          )
        ),
        row(
          "businessdetails.contactingyou.phone.title",
          phoneNumber,
          editAction(
            controllers.businessdetails.routes.ContactingYouPhoneController.get(true).url,
            "businessdetailscontactphone-edit"
          )
        )
      )
    }
  }

  private def correspondenceAddressRows(businessDetails: BusinessDetails)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def lettersAddressRow(label: String) = row(
      "businessdetails.lettersaddress.title",
      messages(label),
      editAction(
        controllers.businessdetails.routes.LettersAddressController.get(true).url,
        "businessdetailslettersaddress-edit"
      )
    )

    def addressIsUKRow(label: String) = row(
      "businessdetails.correspondenceaddress.isuk.title",
      messages(label),
      editAction(
        controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get(true).url,
        "businessdetailscorraddressisuk-edit"
      )
    )

    (businessDetails.altCorrespondenceAddress, businessDetails.correspondenceAddress) match {
      case (Some(false), _) => Some(Seq(lettersAddressRow("lbl.no")))
      case (Some(true), Some(address: CorrespondenceAddress)) if address.isUk.contains(true) =>

        address.ukAddress.map { address =>
          Some(Seq(
            lettersAddressRow("lbl.yes"),
            addressIsUKRow("businessdetails.correspondenceaddress.ukAddress"),
            SummaryListRow(
              Key(
                Text(messages("businessdetails.correspondenceaddress.title"))
              ),
              addressToLines(address.toLines),
              actions = editAction(
                controllers.businessdetails.routes.CorrespondenceAddressUkController.get(true).url,
                "businessdetailscorraddress-edit"
              )
            )
          ))
        }.getOrElse(
          Some(Seq(
            lettersAddressRow("lbl.yes"),
            addressIsUKRow("businessdetails.correspondenceaddress.ukAddress")
          ))
        )
      case (Some(true), Some(address: CorrespondenceAddress)) if address.isUk.contains(false) =>

        address.nonUkAddress.map { address =>
          Some(Seq(
            lettersAddressRow("lbl.no"),
            addressIsUKRow("businessdetails.correspondenceaddress.nonUkAddress"),
            SummaryListRow(
              Key(
                Text(messages("businessdetails.correspondenceaddress.title"))
              ),
              addressToLines(address.toLines),
              actions = editAction(
                controllers.businessdetails.routes.CorrespondenceAddressNonUkController.get(true).url,
                "businessdetailscorraddress-edit"
              )
            )
          ))
        }.getOrElse(
          Some(Seq(
            lettersAddressRow("lbl.no"),
            addressIsUKRow("businessdetails.correspondenceaddress.nonUkAddress")
          ))
        )
      case (_, _) => None
    }
  }
  private def row(title: String, label: String, actions: Option[Actions])(implicit messages: Messages) = {
    SummaryListRow(
      Key(Text(messages(title))),
      Value(Text(label)),
      actions = actions
    )
  }

  private def editAction(route: String, id: String)(implicit messages: Messages) = {
    Some(Actions(
      items = Seq(ActionItem(
        route,
        Text(messages("button.edit")),
        attributes = Map("id" -> id)
      ))
    ))
  }
  private def addressToLines(addressLines: Seq[String]): Value = Value(
    HtmlContent(
      Html(
        "<ul class=\"govuk-list\">" +
          addressLines.map { line =>
            s"""<li>$line<li>"""
          }.mkString
          + "</ul>"
      )
    )
  )
}
