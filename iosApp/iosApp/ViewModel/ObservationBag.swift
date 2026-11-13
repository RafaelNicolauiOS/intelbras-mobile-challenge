/// What an Observable must release when its screen goes away: the tasks that observe the
/// shared ViewModel and the ViewModel's coroutine scope.
///
/// It exists so `deinit` never touches the Observable's main-actor state: the Observable keeps
/// the bag in a `nonisolated let` and both `stop()` (on the main actor) and `deinit` (on whatever
/// thread releases the last reference) only call `bag.stop()`. `@unchecked Sendable` is safe
/// because `stop()` runs on the main actor while the screen is alive, and `deinit` only runs once
/// no one else holds the Observable. Cancelling tasks and a coroutine scope is thread-safe.
final class ObservationBag: @unchecked Sendable {
    private var tasks: [Task<Void, Never>] = []
    private var release: (() -> Void)?
    private(set) var isStopped = false

    /// `release` runs once, on `stop()`; it clears the shared ViewModel.
    init(release: @escaping () -> Void) {
        self.release = release
    }

    func add(_ task: Task<Void, Never>) {
        tasks.append(task)
    }

    /// Idempotent: the coordinator calls it when the screen leaves, `deinit` calls it again.
    func stop() {
        guard !isStopped else { return }
        isStopped = true
        tasks.forEach { $0.cancel() }
        tasks = []
        release?()
        release = nil
    }
}
