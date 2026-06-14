import request from './request';

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  });
}

export function getMaterialList() {
  return request({
    url: '/material/list',
    method: 'get'
  });
}

export function addMaterial(data) {
  return request({
    url: '/material/add',
    method: 'post',
    data
  });
}

export function getCategoryList() {
  return request({
    url: '/category/list',
    method: 'get'
  });
}

export function addCategory(data) {
  return request({
    url: '/category/add',
    method: 'post',
    data
  });
}

export function getRecordList() {
  return request({
    url: '/record/list',
    method: 'get'
  });
}

export function getPendingRecords() {
  return request({
    url: '/record/pending',
    method: 'get'
  });
}

export function applyMaterial(data) {
  return request({
    url: '/record/apply',
    method: 'post',
    data
  });
}

export function applyBatchMaterial(data) {
  return request({
    url: '/record/apply-batch',
    method: 'post',
    data
  });
}

export function approveRecord(data) {
  return request({
    url: '/record/approve',
    method: 'post',
    data
  });
}
