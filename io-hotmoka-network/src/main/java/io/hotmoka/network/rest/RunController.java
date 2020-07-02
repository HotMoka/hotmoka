package io.hotmoka.network.rest;

import io.hotmoka.network.service.run.NodeRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("run")
public class RunController {

    @Autowired
    private NodeRunService nodeRunService;

    @PostMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return this.nodeRunService.runInstanceMethodCallTransaction();
    }

    @PostMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodeRunService.runStaticMethodCallTransaction();
    }
}