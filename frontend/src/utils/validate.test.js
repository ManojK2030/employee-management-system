import { describe, expect, it } from 'vitest';
import { EMPTY_EMPLOYEE, toPayload, validateEmployee } from './validate.js';
import { buildQuery } from '../api/employeeApi.js';

const valid = {
  fullName: 'Priya Sharma',
  email: 'priya.sharma@example.com',
  phoneNumber: '+91 98765 43210',
  department: 'Engineering',
  designation: 'Backend Developer',
  dateOfJoining: '2024-07-01',
  salary: '850000',
  status: 'ACTIVE',
};

describe('validateEmployee', () => {
  it('accepts a fully valid employee', () => {
    expect(validateEmployee(valid)).toEqual({});
  });

  it('requires every field on an empty form', () => {
    const errors = validateEmployee(EMPTY_EMPLOYEE);
    expect(Object.keys(errors).sort()).toEqual(
      ['dateOfJoining', 'department', 'designation', 'email', 'fullName', 'phoneNumber', 'salary'].sort(),
    );
  });

  it('rejects malformed emails', () => {
    expect(validateEmployee({ ...valid, email: 'priya' }).email).toBeDefined();
    expect(validateEmployee({ ...valid, email: 'priya@example' }).email).toBeDefined();
    expect(validateEmployee({ ...valid, email: 'a b@example.com' }).email).toBeDefined();
  });

  it('rejects malformed phone numbers but accepts common formats', () => {
    expect(validateEmployee({ ...valid, phoneNumber: 'abc' }).phoneNumber).toBeDefined();
    expect(validateEmployee({ ...valid, phoneNumber: '123' }).phoneNumber).toBeDefined();
    expect(validateEmployee({ ...valid, phoneNumber: '9876543210' }).phoneNumber).toBeUndefined();
    expect(validateEmployee({ ...valid, phoneNumber: '080-12345678' }).phoneNumber).toBeUndefined();
  });

  it('rejects zero, negative, non-numeric and over-precise salaries', () => {
    expect(validateEmployee({ ...valid, salary: '0' }).salary).toBeDefined();
    expect(validateEmployee({ ...valid, salary: '-100' }).salary).toBeDefined();
    expect(validateEmployee({ ...valid, salary: 'abc' }).salary).toBeDefined();
    expect(validateEmployee({ ...valid, salary: '10.123' }).salary).toBeDefined();
    expect(validateEmployee({ ...valid, salary: '1234.5' }).salary).toBeUndefined();
  });

  it('treats whitespace-only values as missing', () => {
    expect(validateEmployee({ ...valid, fullName: '   ' }).fullName).toBeDefined();
  });

  it('rejects an unknown status', () => {
    expect(validateEmployee({ ...valid, status: 'RETIRED' }).status).toBeDefined();
  });
});

describe('toPayload', () => {
  it('trims strings and converts salary to a number', () => {
    const payload = toPayload({ ...valid, fullName: '  Priya  ', salary: '850000.50' });
    expect(payload.fullName).toBe('Priya');
    expect(payload.salary).toBe(850000.5);
  });
});

describe('buildQuery', () => {
  it('skips empty values and encodes the rest', () => {
    expect(buildQuery({ search: ' priya ', department: '', status: undefined, page: 0, size: 10 })).toBe(
      'search=priya&page=0&size=10',
    );
  });
});
