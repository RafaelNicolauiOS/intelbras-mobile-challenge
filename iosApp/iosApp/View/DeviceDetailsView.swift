import Shared
import SwiftUI

struct DeviceDetailsView: View {
    @ObservedObject var observable: DeviceDetailsObservable

    var body: some View {
        if let device = observable.device {
            List {
                LabeledContent("Nome", value: device.name)
                switch onEnum(of: device) {
                case .alarm(let alarm):
                    LabeledContent("MAC", value: alarm.macAddress)
                case .video(let video):
                    LabeledContent("Serial", value: video.serial)
                    LabeledContent("Usuário", value: video.username)
                }
            }
        } else {
            Text("Dispositivo não encontrado")
        }
    }
}
