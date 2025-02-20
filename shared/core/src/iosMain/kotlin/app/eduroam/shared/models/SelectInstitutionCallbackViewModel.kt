package app.eduroam.shared.models


import app.eduroam.shared.config.ConfigParser
import app.eduroam.shared.select.InstitutionsRepository
import app.eduroam.shared.select.SelectInstitutionViewModel
import co.touchlab.kermit.Logger
import app.eduroam.shared.models.CallbackViewModel

@Suppress("Unused") // Members are called from Swift
class SelectInstitutionCallbackViewModel(
    private val institutionRepository: InstitutionsRepository,
    private val configParser: ConfigParser,
    log: Logger,
) : CallbackViewModel() {

    override val viewModel = SelectInstitutionViewModel(institutionRepository, configParser, log)

    val institutions = viewModel.uiDataState.asCallbacks()

    fun onSearchTextChange(search: String) {
        viewModel.onSearchTextChange(search)
    }
}
