package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.scanReduce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun loadContributorsReactive(
    service: GitHubService,
    req: RequestData
): Flow<List<User>> = channelFlow {
    withContext(Dispatchers.IO) {
        val repos = service.getOrgRepos(req.org)
        for (repo in repos) {
            launch {
                val users = service.getRepoContributors(req.org, repo.name)
                send(users)
            }
        }
    }
}.scanReduce { accumulator, value -> (accumulator + value).aggregate() }