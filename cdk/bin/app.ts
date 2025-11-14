#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { BakehouseStack } from '../lib/bakehouse-stack';

const app = new cdk.App();

new BakehouseStack(app, 'BakehouseStack', {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION || 'us-east-1',
  },
  description: 'The Bake House - Sales Tracking Application Infrastructure',
});

app.synth();
