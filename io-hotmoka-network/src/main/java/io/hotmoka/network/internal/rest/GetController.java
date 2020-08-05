package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.services.GetService;
import io.hotmoka.network.models.requests.TransactionRestRequestModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("get")
public class GetController {

    @Autowired
    private GetService nodeGetService;

    @GetMapping("/takamakaCode")
    public @ResponseBody TransactionReferenceModel getTakamakaCode() {
        return nodeGetService.getTakamakaCode();
    }

    @GetMapping("/manifest")
    public @ResponseBody StorageReferenceModel getManifest() {
        return nodeGetService.getManifest();
    }

    @PostMapping("/state")
    public @ResponseBody StateModel getState(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getState(request);
    }

    @PostMapping("/classTag")
    public @ResponseBody ClassTagModel getClassTag(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getClassTag(request);
    }

    @PostMapping("/requestAt")
    public @ResponseBody TransactionRestRequestModel<?> getRequestAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getRequestAt(reference);
    }

    @PostMapping("/responseAt")
    public @ResponseBody TransactionRestResponseModel<?> getResponseAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getResponseAt(reference);
    }

    @PostMapping("/polledResponseAt")
    public @ResponseBody TransactionRestResponseModel<?> getPolledResponseAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getPolledResponseAt(reference);
    }

    @GetMapping("/signatureAlgorithmForRequests")
    public @ResponseBody String getSignatureAlgorithmForRequests() {
        return nodeGetService.getSignatureAlgorithmForRequests();
    }
}