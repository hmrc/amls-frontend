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

package forms.declaration

import forms.mappings.Mappings
import models.declaration.AddPerson
import models.declaration.release7.{Other, RoleType, RoleWithinBusinessRelease7}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class AddPersonFormProvider @Inject() () extends Mappings {

  val nameLength  = 35
  val otherLength = 255

  private val emptyError = "error.invalid.position.validation"

  def apply(): Form[AddPerson] = Form[AddPerson](
    mapping(
      "firstName"     -> text("error.required.declaration.first_name").verifying(
        firstError(
          maxLength(nameLength, "error.invalid.firstname.length"),
          regexp(nameRegex, "error.invalid.firstname.validation")
        )
      ),
      "middleName"    -> optional(
        text().verifying(
          firstError(
            maxLength(nameLength, "error.invalid.middlename.length"),
            regexp(nameRegex, "error.invalid.middlename.validation")
          )
        )
      ),
      "lastName"      -> text("error.required.declaration.last_name").verifying(
        firstError(
          maxLength(nameLength, "error.invalid.lastname.length"),
          regexp(nameRegex, "error.invalid.lastname.validation")
        )
      ),
      "positions"     -> seq(enumerable[RoleType](emptyError, emptyError)(RoleWithinBusinessRelease7.enumerable)).verifying(
        nonEmptySeq(emptyError)
      ),
      "otherPosition" -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("error.required.declaration.specify.role").verifying(
          firstError(
            maxLength(otherLength, "error.invalid.role.in.business.maxlength.255"),
            regexp(basicPunctuationRegex, "err.text.role.in.business.text.validation")
          )
        )
      )
    )(apply)(unapply)
  )

  def apply(
    firstName: String,
    middleName: Option[String],
    lastName: String,
    positions: Seq[RoleType],
    otherPosition: Option[String]
  ): AddPerson = {
    val role = (positions, otherPosition) match {
      case (pos, Some(other)) if pos.contains(Other("")) =>
        val modifiedTransactions = pos.map(role => if (role == Other("")) Other(other) else role)
        RoleWithinBusinessRelease7(modifiedTransactions.toSet)
      case (pos, Some(_))                                => throw new IllegalArgumentException("Role description requires the selection of Other")
      case (pos, None) if pos.contains(Other(""))        =>
        throw new IllegalArgumentException("Selection of Other requires a role description")
      case (pos, None)                                   => RoleWithinBusinessRelease7(pos.toSet)
    }
    AddPerson(firstName, middleName, lastName, role)
  }

  def unapply(obj: AddPerson): Option[(String, Option[String], String, Seq[RoleType], Option[String])] = {
    val roleSet   = obj.roleWithinBusiness.items.map(x => if (x.value == Other("").value) Other("") else x)
    val roleOther = obj.roleWithinBusiness.items.collectFirst { case o: Other =>
      o.details
    }
    Some((obj.firstName, obj.middleName, obj.lastName, roleSet.toSeq, roleOther))
  }
}
