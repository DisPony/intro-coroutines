package contributors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private val INSETS = Insets(3, 10, 3, 10)
private val COLUMNS = arrayOf("Login", "Contributions")

@OptIn(ExperimentalTime::class)
@Suppress("CONFLICTING_INHERITED_JVM_DECLARATIONS")
class ContributorsView : JFrame("GitHub Contributors"), CoroutineScope {
    private val username = JTextField(20)
    private val password = JPasswordField(20)
    private val org = JTextField(20)
//    private val variant = JComboBox<Variant>(Variant.values())
    private val load = JButton("Load contributors")
    private val cancel = JButton("Cancel").apply { isEnabled = false }

    private val resultsModel = DefaultTableModel(COLUMNS, 0)
    private val results = JTable(resultsModel)
    private val resultsScroll = JScrollPane(results).apply {
        preferredSize = Dimension(200, 200)
    }

    private val viewModel = ContributorsViewModel()

    private val loadingIcon = ImageIcon(javaClass.classLoader.getResource("ajax-loader.gif"))
    private val loadingStatus = JLabel("Start new loading", loadingIcon, SwingConstants.CENTER)
    private val loadingTime = JLabel("")

    val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init {
        // Create UI
        rootPane.contentPane = JPanel(GridBagLayout()).apply {
            addLabeled("GitHub Username", username)
            addLabeled("Password/Token", password)
            addWideSeparator()
            addLabeled("Organization", org)
//            addLabeled("Variant", variant)
            addWideSeparator()
            addWide(JPanel().apply {
                add(load)
                add(cancel)
            })
            addWide(resultsScroll) {
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
            addWide(JPanel().apply {
                add(loadingStatus)
                add(loadingTime)
            })
        }

        load.addActionListener {
            saveParams()
            val (username, password, org) = getParams()
            val req = RequestData(username, password, org)
            viewModel.onStartLoading(req)
        }

        cancel.addActionListener {
            viewModel.onCancel()
        }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                job.cancel()
                saveParams()
                exitProcess(0)
            }
        })


        setParams(loadStoredParams())

        launch(Dispatchers.Main) {
            for (status in viewModel.loadingStatus.openSubscription()) {
                loadingStatus.text = "$status"
                loadingStatus.icon = if (status == LoadingStatus.IN_PROGRESS) loadingIcon else null
                if(status == LoadingStatus.CANCELED) loadingTime.text = ""
            }
        }

        launch {
            for(contribution in viewModel.contributions.openSubscription()){
                updateContributors(contribution)
            }
        }

        launch {
            for(duration in viewModel.timeSpent.openSubscription()){
                loadingTime.text = duration.toString(DurationUnit.SECONDS, 1)
            }
        }

        launch {
            for (cancelable in viewModel.isCancelable.openSubscription()){
                cancel.isEnabled = cancelable
            }
        }

        launch {
            viewModel.newLoadAvailable.collect { load.isEnabled = it }
        }
    }

//    private fun getSelectedVariant(): Variant = variant.getItemAt(variant.selectedIndex)

    private fun updateContributors(users: List<User>) {
        if (users.isNotEmpty()) {
            log.info("Updating result with ${users.size} rows")
        }
        else {
            log.info("Clearing result")
        }
        resultsModel.setDataVector(users.map {
            arrayOf(it.login, it.contributions)
        }.toTypedArray(), COLUMNS)
    }

    private fun getParams(): Params {
        return Params(username.text, password.password.joinToString(""), org.text, Variant.REACTIVE)
    }

    private fun saveParams() {
        val params = getParams()
        if (params.username.isEmpty() && params.password.isEmpty()) {
            removeStoredParams()
        }
        else {
            saveParams(params)
        }
    }

    private fun setParams(params: Params) {
        username.text = params.username
        password.text = params.password
        org.text = params.org
//        variant.selectedIndex = params.variant.ordinal
    }

}

