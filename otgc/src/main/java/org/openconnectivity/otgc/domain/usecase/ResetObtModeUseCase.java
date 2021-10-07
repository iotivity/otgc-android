package org.openconnectivity.otgc.domain.usecase;

import org.openconnectivity.otgc.data.repository.DoxsRepository;
import org.openconnectivity.otgc.data.repository.IotivityRepository;
import org.openconnectivity.otgc.data.repository.PreferencesRepository;
import org.openconnectivity.otgc.data.repository.ProvisioningRepository;
import org.openconnectivity.otgc.utils.constant.OtgcMode;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Completable;

public class ResetObtModeUseCase {

    private final IotivityRepository iotivityRepository;
    private final DoxsRepository doxsRepository;
    private final ProvisioningRepository provisioningRepository;
    private final PreferencesRepository preferencesRepository;

    @Inject
    public ResetObtModeUseCase(IotivityRepository iotivityRepository,
                               DoxsRepository doxsRepository,
                               ProvisioningRepository provisioningRepository,
                               PreferencesRepository preferencesRepository) {
        this.iotivityRepository = iotivityRepository;
        this.doxsRepository = doxsRepository;
        this.provisioningRepository = provisioningRepository;
        this.preferencesRepository = preferencesRepository;
    }

    public Completable execute() {
        return iotivityRepository.scanOwnedDevices()
                .flatMapCompletable(device -> doxsRepository.resetDevice(device.getDeviceId()))
                .delay(preferencesRepository.getRequestsDelay(), TimeUnit.SECONDS)
                .andThen(Completable.fromAction(() -> preferencesRepository.setMode(OtgcMode.OBT)))
                .andThen(provisioningRepository.resetSvrDb())
                .andThen(provisioningRepository.doSelfOwnership());
    }
}
