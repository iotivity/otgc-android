package org.openconnectivity.otgc.domain.usecase.cloud;

import io.reactivex.Completable;
import org.iotivity.OCCredUsage;
import org.iotivity.OCCredUtil;
import org.openconnectivity.otgc.data.repository.*;
import org.openconnectivity.otgc.domain.model.devicelist.Device;
import org.openconnectivity.otgc.domain.model.resource.secure.cred.OcCredential;
import org.openconnectivity.otgc.domain.model.resource.secure.cred.OcCredentials;
import org.openconnectivity.otgc.utils.rx.SchedulersFacade;
import org.openconnectivity.otgc.domain.model.resource.cloud.OcCloudConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

public class RegisterDeviceCloudUseCase {

    private final IotivityRepository iotivityRepository;
    private final CloudRepository cloudRepository;
    private final AmsRepository amsRepository;
    private final CmsRepository cmsRepository;

    @Inject
    public RegisterDeviceCloudUseCase(IotivityRepository iotivityRepository,
                                      AmsRepository amsRepository,
                                      CloudRepository cloudRepository,
                                      CmsRepository cmsRepository) {
        this.iotivityRepository = iotivityRepository;
        this.amsRepository = amsRepository;
        this.cloudRepository = cloudRepository;
        this.cmsRepository = cmsRepository;
    }

    public Completable execute(Device deviceToRegister, String accessToken) {

        //int delay = Integer.parseInt(settingRepository.get(SettingRepository.REQUESTS_DELAY_KEY, SettingRepository.REQUESTS_DELAY_DEFAULT_VALUE));

        OcCloudConfiguration configuration = cloudRepository.retrieveCloudConfiguration().blockingGet();


        OcCredentials creds = cmsRepository.retrieveOwnCredentials().blockingGet();
        for(OcCredential cred : creds.getCredList()) {
            if (cred.getCredusage() != null && !cred.getCredusage().isEmpty()
                    && (OCCredUtil.parseCredUsage(cred.getCredusage()) == OCCredUsage.OC_CREDUSAGE_MFG_TRUSTCA
                    || OCCredUtil.parseCredUsage(cred.getCredusage()) == OCCredUsage.OC_CREDUSAGE_TRUSTCA)) {
                byte[] pem = cred.getPublicData().getPemData().getBytes();
                cmsRepository.provisionTrustAnchor(pem, configuration.getCloudUuid(), deviceToRegister.getDeviceId()).blockingGet();
            }
        }

        return  amsRepository.provisionConntypeAce(deviceToRegister.getDeviceId(), true, Arrays.asList("/CoapCloudConfResURI"), 6).
                andThen(cmsRepository.registerDeviceCloud(deviceToRegister.getDeviceId(), "/CoapCloudConfResURI", configuration.getAuthProvider(), configuration.getCloudUrl(),
                        configuration.getCloudUuid(), accessToken));
    }
}
