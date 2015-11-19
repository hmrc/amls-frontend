package forms

import config.AmlsPropertiesReader._
import models.BusinessHasWebsite
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanWithTextValidator._

class AboutTheBusinessForms {

  val businessHasWebsiteFormMapping = mapping(
    "hasWebsite" -> mandatoryBooleanWithText("website", getProperty("roleForBusiness").split(",").reverse.head,
      "error.required", "error.required", "error.invalid", "", "validationMaxLengthRoleForBusinessOther"),
    "website" -> text
  )(BusinessHasWebsite.apply)(BusinessHasWebsite.unapply)

  val businessHasWebsiteForm = Form(businessHasWebsiteFormMapping)


}
