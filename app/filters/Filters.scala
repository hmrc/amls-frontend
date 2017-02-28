import javax.inject.Inject

import filters.ConfirmationFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject()(confirmationFilter: ConfirmationFilter) extends DefaultHttpFilters(confirmationFilter)

