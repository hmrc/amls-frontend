/*
 * Copyright 2017 HM Revenue & Customs
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

package models.registrationdetails

import org.joda.time.LocalDate
import play.api.libs.json._

sealed trait OrganisationType
case object Partnership extends OrganisationType
case object LLP extends OrganisationType
case object CorporateBody extends OrganisationType
case object UnincorporatedBody extends OrganisationType

object OrganisationType {
  implicit val reads = new Reads[OrganisationType] {
    override def reads(json: JsValue) = json match {
      case JsString("Partnership") => JsSuccess(Partnership)
      case JsString("LLP") => JsSuccess(LLP)
      case JsString("Corporate body") => JsSuccess(CorporateBody)
      case JsString("Unincorporated body") => JsSuccess(UnincorporatedBody)
      case x => JsError(s"Unable to parse the organisation type value: $x")
    }
  }
}

sealed trait OrganisationBodyDetails
case class Organisation(organisationName: String, isAGroup: Boolean, organisationType: OrganisationType) extends OrganisationBodyDetails

object Organisation {
  implicit val orgReads = Json.reads[Organisation]
}

case class Individual(firstName: String, middleName: Option[String], lastName: String, dateOfBirth: LocalDate) extends OrganisationBodyDetails

object Individual {
  implicit val indReads = Json.reads[Individual]
}

object OrganisationBodyDetails {
  implicit val reads: Reads[OrganisationBodyDetails] = {
    import play.api.libs.json._
    (__ \ "isAnIndividual").read[Boolean] flatMap {
      case true => (__ \ "individual").read[Individual].map(identity[OrganisationBodyDetails])
      case _ => (__ \ "organisation").read[Organisation].map(identity[OrganisationBodyDetails])
    }
  }
}

case class RegistrationDetails(isAnIndividual: Boolean, bodyDetails: OrganisationBodyDetails)

object RegistrationDetails {
  implicit val reads: Reads[RegistrationDetails] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "isAnIndividual").read[Boolean] and
        __.read[OrganisationBodyDetails]
    )(RegistrationDetails.apply _)
  }
}
